/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.test.internal.constraintvalidators.hv;

import static java.lang.annotation.ElementType.METHOD;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertCorrectConstraintViolationMessages;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertNumberOfViolations;
import static org.hibernate.validator.testutils.ValidatorUtil.getConfiguration;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.EmailDef;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.hibernate.validator.testutil.MyCustomStringImpl;
import org.hibernate.validator.testutil.TestForIssue;
import org.hibernate.validator.testutils.ValidatorUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Hardy Ferentschik
 * @author Guillaume Smet
 */
public class EmailValidatorTest {
	// http://stackoverflow.com/questions/406230/regular-expression-to-match-string-not-containing-a-word
	private static final String noOrgEmailAddressRegexp = "^((?!\\.org).)*$";
	private static EmailValidator validator;


	@BeforeClass
	public static void init() {
		validator = new EmailValidator();
	}

	@Test
	public void testNullAndEmptyString() throws Exception {
		isValidEmail( "" );
		isValidEmail( null );
	}

	@Test
	public void testValidEmail() throws Exception {
		isValidEmail( "emmanuel@hibernate.org" );
		isValidEmail( "emmanuel@hibernate" );
		isValidEmail( "emma-n_uel@hibernate" );
		isValidEmail( "emma+nuel@hibernate.org" );
		isValidEmail( "emma=nuel@hibernate.org" );
		isValidEmail( "emmanuel@[123.12.2.11]" );
		isValidEmail( "*@example.net" );
		isValidEmail( "fred&barny@example.com" );
		isValidEmail( "---@example.com" );
		isValidEmail( "foo-bar@example.net" );
		isValidEmail( "mailbox.sub1.sub2@this-domain" );
	}

	@Test
	public void testInValidEmail() throws Exception {
		isInvalidEmail( "emmanuel.hibernate.org" );
		isInvalidEmail( "emma nuel@hibernate.org" );
		isInvalidEmail( "emma(nuel@hibernate.org" );
		isInvalidEmail( "emmanuel@" );
		isInvalidEmail( "emma\nnuel@hibernate.org" );
		isInvalidEmail( "emma@nuel@hibernate.org" );
		isInvalidEmail( "emma@nuel@.hibernate.org" );
		isInvalidEmail( "Just a string" );
		isInvalidEmail( "string" );
		isInvalidEmail( "me@" );
		isInvalidEmail( "@example.com" );
		isInvalidEmail( "me.@example.com" );
		isInvalidEmail( ".me@example.com" );
		isInvalidEmail( "me@example..com" );
		isInvalidEmail( "me\\@example.com" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-339")
	public void testAccent() {
		isValidEmail( "Test^Email@example.com" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-502")
	public void testValidEmailCharSequence() throws Exception {
		isValidEmail( new MyCustomStringImpl( "emmanuel@hibernate.org" ) );
		isInvalidEmail( new MyCustomStringImpl( "@example.com" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HV-472")
	public void testMailWithInternationalDomainName() throws Exception {
		isValidEmail( "myname@??stereich.at", "A valid email address with umlaut" );
		isValidEmail( "????????@??????????????.??????", "A valid greek email address" );
		isInvalidEmail( "????????.??????????????.??????", "Email does not contain an @ character and should be invalid" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-554")
	public void testEmailRegExp() {
		final String email = "hardy@hibernate.org";

		// ensure the plain email is valid
		isValidEmail( email );


		// add additional regexp constraint to email
		Validator validator = ValidatorUtil.getValidator();
		EmailContainer container = new EmailContainerAnnotated();
		container.setEmail( email );
		Set<ConstraintViolation<EmailContainer>> violations = validator.validate( container );
		assertOrgAddressesAreNotValid( violations );

		// now the same test with programmatic configuration
		final HibernateValidatorConfiguration config = getConfiguration( HibernateValidator.class );
		ConstraintMapping mapping = config.createConstraintMapping();
		mapping.type( EmailContainer.class )
				.property( "email", METHOD )
				.constraint(
						new EmailDef().regexp( noOrgEmailAddressRegexp )
								.message( "ORG addresses are not valid" )
				);
		config.addMapping( mapping );
		validator = config.buildValidatorFactory().getValidator();

		container = new EmailContainerNoAnnotations();
		container.setEmail( email );
		violations = validator.validate( container );
		assertOrgAddressesAreNotValid( violations );
	}

	@Test
	@TestForIssue(jiraKey = "HV-591")
	public void testEmailAddressLength() {
		isValidEmail( "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@hibernate.org" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-810")
	public void testEMailWithTrailingAt() throws Exception {
		isInvalidEmail( "validation@hibernate.com@" );
		isInvalidEmail( "validation@hibernate.com@@" );
		isInvalidEmail( "validation@hibernate.com@@@" );
	}

	@Test
	@TestForIssue(jiraKey = { "HV-1005", "HV-1066" })
	public void testEmailWithUpTo64CharacterLocalPartIsValid() {
		// Local part should allow up to 64 octets: https://tools.ietf.org/html/rfc5321#section-4.5.3.1.1
		for ( int length = 1; length <= 64; length++ ) {
			isValidEmail( stringOfLength( length ) + "@foo.com" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HV-1005")
	public void testEmailWith65CharacterLocalPartIsInvalid() {
		isInvalidEmail( stringOfLength( 65 ) + "@foo.com" );
	}

	@Test
	@TestForIssue(jiraKey = { "HV-1005", "HV-1066" })
	public void testEmailWithUpTo255CharacterDomainPartIsValid() {
		// Domain part should allow up to 255
		for ( int length = 1; length <= 251; length++ ) {
			isValidEmail( "foo@" + domainOfLength( length ) + ".com" );
		}
	}

	@Test
	@TestForIssue(jiraKey = { "HV-1005", "HV-1066" })
	public void testEmailWith63CharactersDomainPartIsValid() {
		isValidEmail( "foo@" + stringOfLength( 63 ) + "." + stringOfLength( 63 ) + ".com" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-1005")
	public void testEmailWith256CharacterDomainPartIsInvalid() {
		// Domain part should allow up to 255
		isInvalidEmail( "foo@" + domainOfLength( 252 ) + ".com" );
	}

	private String stringOfLength(int length) {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < length; i++ ) {
			builder.append( 'a' );
		}
		String s = builder.toString();
		assertEquals( length, s.getBytes().length );
		return s;
	}

	private String domainOfLength(int length) {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < length; i++ ) {
			// we insert a dot from time to time to be sure each label of the domain name is at most 63 characters long
			if ( i % 32 == 0 && i > 0 && i < length - 1 ) {
				builder.append( "." );
			}
			else {
				builder.append( 'a' );
			}
		}
		String s = builder.toString();
		assertEquals( length, s.getBytes().length );
		return s;
	}

	private void assertOrgAddressesAreNotValid(Set<ConstraintViolation<EmailContainer>> violations) {
		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "ORG addresses are not valid" );
	}

	private void isValidEmail(CharSequence email, String message) {
		assertTrue( validator.isValid( email, null ), String.format( message, email ) );
	}

	private void isValidEmail(CharSequence email) {
		isValidEmail( email, "Expected %1$s to be a valid email." );
	}

	private void isInvalidEmail(CharSequence email, String message) {
		assertFalse( validator.isValid( email, null ), String.format( message, email ) );
	}

	private void isInvalidEmail(CharSequence email) {
		isInvalidEmail( email, "Expected %1$s to be an invalid email." );
	}

	@SuppressWarnings("unused")
	private abstract static class EmailContainer {
		public String email;

		public void setEmail(String email) {
			this.email = email;
		}

		public String getEmail() {
			return email;
		}
	}

	private static class EmailContainerAnnotated extends EmailContainer {
		@Override
		@Email(regexp = EmailValidatorTest.noOrgEmailAddressRegexp, message = "ORG addresses are not valid")
		public String getEmail() {
			return email;
		}
	}

	private static class EmailContainerNoAnnotations extends EmailContainer {
	}
}
