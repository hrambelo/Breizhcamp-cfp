package controllers;

import models.User;
import models.utils.AppException;
import models.utils.TransformValidationErrors;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;

import static play.libs.Json.toJson;

/**
 * Login and Logout.
 * User: yesnault
 */
public class Application extends Controller {

    /**
     * Login class used by Login Form.
     */
    public static class Login {

        @Constraints.Required
        public String email;
        @Constraints.Required
        public String password;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {

            User user;
            try {
                user = User.authenticate(email, password);
            } catch (AppException e) {
                return Messages.get("error.technical");
            }
            if (user == null) {
                return Messages.get("invalid.user.or.password");
            } else if (!user.validated) {
                return Messages.get("account.not.validated.check.mail");
            }
            return null;
        }

    }

    public static class Register {

        @Constraints.Required
        public String email;

        @Constraints.Required
        public String fullname;

        @Constraints.Required
        public String inputPassword;

        @Constraints.Required
        public String confirmPassword;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {
            if (isBlank(email)) {
                return "Email is required";
            }

            if (isBlank(fullname)) {
                return "Full name is required";
            }

            if (isBlank(inputPassword)) {
                return "Password is required";
            }

            if (isBlank(confirmPassword)) {
                return "Confirm Password is required";
            }

            if (!inputPassword.equals(confirmPassword)) {
                return "Passwords do not match";
            }

            return null;
        }

        private boolean isBlank(String input) {
            return input == null || input.isEmpty() || input.trim().isEmpty();
        }
    }

    /**
     * Handle login form submission.
     *
     * @return User if auth OK or 403 ?? if auth KO
     */
    // Utilisée par le js.
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        if (loginForm.hasErrors()) {
        	return unauthorized(toJson(TransformValidationErrors.transform(loginForm.errors())));
        } else {
            session("email", loginForm.get().email);
            return ok(toJson(User.findByEmail(loginForm.get().email)));
        }
    }

    public static final String FLASH_MESSAGE_KEY = "message";
    public static final String FLASH_ERROR_KEY = "error";

    public static Result oAuthDenied(final String providerKey) {
        //com.feth.play.module.pa.controllers.Authenticate.noCache(response());
        flash(FLASH_ERROR_KEY,
                "You need to accept the OAuth connection in order to use this website!");
        return null; //redirect(routes.Application.index());
    }

    public static Result index(){
        return redirect("/");
    }

    /**
     * Logout and clean the session.
     *
     * @return Index page
     */
    // Utilisée par le js.
    public static Result logout() {
        session().clear();
        flash("success", Messages.get("youve.been.logged.out"));
        return ok();
    }

}