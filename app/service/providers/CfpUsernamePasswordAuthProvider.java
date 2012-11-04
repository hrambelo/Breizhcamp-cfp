package service.providers;


import com.feth.play.module.mail.Mailer;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import controllers.account.Signup;
import controllers.routes;
import models.LinkedAccount;
import models.User;
import org.apache.commons.mail.EmailException;
import play.Application;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Http;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CfpUsernamePasswordAuthProvider extends
        UsernamePasswordAuthProvider<String, CfpLoginUsernamePasswordAuthUser, CfpUsernamePasswordAuthUser, CfpUsernamePasswordAuthProvider.MyLogin, CfpUsernamePasswordAuthProvider.MySignup> {

    private static final String SETTING_KEY_VERIFICATION_LINK_SECURE = SETTING_KEY_MAIL
            + "." + "verificationLink.secure";
    private static final String SETTING_KEY_PASSWORD_RESET_LINK_SECURE = SETTING_KEY_MAIL
            + "." + "passwordResetLink.secure";
    private static final String SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET = "loginAfterPasswordReset";

    @Override
    protected List<String> neededSettingKeys() {
        final List<String> needed = new ArrayList<String>(
                super.neededSettingKeys());
        needed.add(SETTING_KEY_VERIFICATION_LINK_SECURE);
        needed.add(SETTING_KEY_PASSWORD_RESET_LINK_SECURE);
        needed.add(SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET);
        return needed;
    }

    public static CfpUsernamePasswordAuthProvider getProvider() {
        return (CfpUsernamePasswordAuthProvider) PlayAuthenticate
                .getProvider(UsernamePasswordAuthProvider.PROVIDER_KEY);
    }

    public static class MyIdentity {

        public MyIdentity() {
        }

        public MyIdentity(final String email) {
            this.email = email;
        }

        @Constraints.Required
        @Constraints.Email
        public String email;

    }

    public static class MyLogin extends MyIdentity
            implements
            com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.UsernamePassword {

        @Constraints.Required
        public String inputPassword;

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getPassword() {
            return inputPassword;
        }
    }

    public static class MySignup extends MyLogin {

        @Constraints.Required
        public String confirmPassword;

        @Constraints.Required
        public String fullname;

        public String validate() {
            if (inputPassword == null || !inputPassword.equals(confirmPassword)) {
                return Messages
                        .get("playauthenticate.password.signup.error.passwords_not_same");
            }
            return null;
        }
    }

    public static final Form<MySignup> SIGNUP_FORM = Controller
            .form(MySignup.class);
    public static final Form<MyLogin> LOGIN_FORM = Controller
            .form(MyLogin.class);

    public CfpUsernamePasswordAuthProvider(Application app) {
        super(app);
    }

    protected Form<MySignup> getSignupForm() {
        return SIGNUP_FORM;
    }

    protected Form<MyLogin> getLoginForm() {
        return LOGIN_FORM;
    }

    @Override
    protected SignupResult signupUser(final CfpUsernamePasswordAuthUser user) {
        final User u = User.findByUsernamePasswordIdentity(user);
        if (u != null) {
            if (u.validated) {
                // This user exists, has its email validated and is active
                return SignupResult.USER_EXISTS;
            } else {
                // this user exists, is active but has not yet validated its
                // email
                sendVerifyEmailMailingAfterSignup(u,null);
                return SignupResult.USER_EXISTS_UNVERIFIED;
            }
        }
        // The user either does not exist or is inactive - create a new one
        @SuppressWarnings("unused")
        final User newUser = User.create(user);
        // Usually the email should be verified before allowing login, however
        // if you return
        // return SignupResult.USER_CREATED;
        // then the user gets logged in directly
        sendVerifyEmailMailingAfterSignup(newUser,null);
        return SignupResult.USER_CREATED_UNVERIFIED;
    }

    @Override
    protected LoginResult loginUser(
            final CfpLoginUsernamePasswordAuthUser authUser) {
        final User u = User.findByUsernamePasswordIdentity(authUser);
        if (u == null) {
            return LoginResult.NOT_FOUND;
        } else {
            if (!u.validated) {
                return LoginResult.USER_UNVERIFIED;
            } else {
                for (final LinkedAccount acc : u.linkedAccounts) {
                    if (getKey().equals(acc.providerKey)) {
                        if (authUser.checkPassword(acc.providerUserId,
                                authUser.getPassword())) {
                            // Password was correct
                            return LoginResult.USER_LOGGED_IN;
                        } else {
                            // if you don't return here,
                            // you would allow the user to have
                            // multiple passwords defined
                            // usually we don't want this
                            return LoginResult.WRONG_PASSWORD;
                        }
                    }
                }
                return LoginResult.WRONG_PASSWORD;
            }
        }
    }

    @Override
    protected Call userExists(final UsernamePasswordAuthUser authUser) {
        //return routes.Signup.exists();
        return routes.Application.index();
    }

    @Override
    protected Call userUnverified(final UsernamePasswordAuthUser authUser) {
        return routes.Application.index();
    }

    @Override
    protected CfpUsernamePasswordAuthUser buildSignupAuthUser(
            final MySignup signup, final Http.Context ctx) {
        return new CfpUsernamePasswordAuthUser(signup);
    }

    @Override
    protected CfpLoginUsernamePasswordAuthUser buildLoginAuthUser(
            final MyLogin login, final Http.Context ctx) {
        return new CfpLoginUsernamePasswordAuthUser(login.getPassword(),
                login.getEmail());
    }

    @Override
    protected String getVerifyEmailMailingSubject(
            final CfpUsernamePasswordAuthUser user, final Http.Context ctx) {
        return Messages.get("playauthenticate.password.verify_signup.subject");
    }

    @Override
    protected String onLoginUserNotFound(final Http.Context context) {
        context.flash()
                .put(controllers.Application.FLASH_ERROR_KEY,
                        Messages.get("playauthenticate.password.login.unknown_user_or_pw"));
        return super.onLoginUserNotFound(context);
    }

    @Override
    protected Mailer.Mail.Body getVerifyEmailMailingBody(final String token,
                                             final CfpUsernamePasswordAuthUser user, final Http.Context ctx) {

        //TODO
        return null;
    }

    private static String generateToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected String generateVerificationRecord(
            final CfpUsernamePasswordAuthUser user) {
        return generateVerificationRecord(User.findByAuthUserIdentity(user));
    }

    protected String generateVerificationRecord(final User user) {
        final String token = generateToken();
        //TODO
        return token;
    }

    protected String generatePasswordResetRecord(final User u) {
        final String token = generateToken();
       //TODO
        return token;
    }



    public void sendPasswordResetMailing(final User user, final Http.Context ctx) {
       //TODO
    }

    public boolean isLoginAfterPasswordReset() {
        return getConfiguration().getBoolean(
                SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET);
    }





    public void sendVerifyEmailMailingAfterSignup(final User user,
                                                  final Http.Context ctx) {
        try {
            Signup.sendMailAskForConfirmation(user);
        } catch (EmailException e) {
            Logger.error(""+e);
        } catch (MalformedURLException e) {
            Logger.error(""+e);
        }
    }

    private String getEmailName(final User user) {
        return getEmailName(user.email, user.fullname);
    }
}
