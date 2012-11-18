package controllers.account;

import models.User;
import models.utils.AppException;
import models.utils.Mail;
import models.utils.TransformValidationErrors;
import org.apache.commons.mail.EmailException;
import org.codehaus.jackson.JsonNode;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import service.providers.CfpUsernamePasswordAuthProvider;

import java.net.MalformedURLException;
import java.net.URL;

import static play.libs.Json.toJson;

/**
 * Signup to Play20StartApp : save and send confirm mail.
 * <p/>
 * User: yesnault
 * Date: 31/01/12
 */
public class Signup extends Controller {

    /**
     * Save the new user.
     *
     * @return Successfull page or created form if bad
     */
    public static Result save() {
        JsonNode newUser = request().body().asJson();
        Form<CfpUsernamePasswordAuthProvider.MySignup> registerForm;
        if (newUser == null) {
            registerForm = CfpUsernamePasswordAuthProvider.SIGNUP_FORM.bindFromRequest();
        } else {
            registerForm = CfpUsernamePasswordAuthProvider.SIGNUP_FORM.bind(newUser);
        }


        if (registerForm.hasErrors()) {
            // User did not fill everything properly
            return badRequest(toJson(TransformValidationErrors.transform(registerForm.errors())));
        } else {
            // Everything was filled
            // do something with your part of the form before handling the user
            // signup
            CfpUsernamePasswordAuthProvider.handleSignup(ctx());
            return ok();
        }
    }

    /**
     * Check if the email already exists.
     *
     * @param email email address
     * @return Index if there was a problem, null otherwise
     */
    private static Result checkBeforeSave(String email) {
        // Check unique email
        if (User.findByEmail(email) != null) {
            return badRequest(toJson(TransformValidationErrors.transform(Messages.get("error.email.already.exist"))));
        }

        return null;
    }

    /**
     * Send the welcome Email with the link to confirm.
     *
     * @param user user created
     * @throws EmailException Exception when sending mail
     */
    public static void sendMailAskForConfirmation(User user) throws EmailException, MalformedURLException {
        String subject = Messages.get("mail.confirm.subject");
        String urlString = "http://" + Configuration.root().getString("server.hostname");
        urlString += "/#/confirm/" + user.confirmationToken;
        URL url = new URL(urlString); // validate the URL, will throw an exception if bad.
        String message = Messages.get("mail.confirm.message", url.toString());

        Mail.Envelop envelop = new Mail.Envelop(subject, message, user.email);
        Mail.sendMail(envelop);
    }

    /**
     * Valid an account with the url in the confirm mail.
     *
     * @param token a token attached to the user we're confirming.
     * @return Confirmationpage
     */
    public static Result confirm(String token) {
        User user = User.findByConfirmationToken(token);
        if (user == null) {
            Logger.debug("Signup.confirm cannot find user");
            return badRequest();
        }

        if (user.validated) {
            Logger.debug("Signup.confirm user allready confirm");
            return badRequest();
        }

        try {
            if (User.confirm(user)) {
                sendMailConfirmation(user);
                return ok();
            } else {
                Logger.debug("Signup.confirm cannot confirm user");
                return badRequest();
            }
        } catch (AppException e) {
            Logger.error("Cannot signup", e);
        } catch (EmailException e) {
            Logger.debug("Cannot send email", e);
        }
        return badRequest();
    }

    public Result signup(){
        return ok();
    }

    /**
     * Send the confirm mail.
     *
     * @param user user created
     * @throws EmailException Exception when sending mail
     */
    private static void sendMailConfirmation(User user) throws EmailException {
        String subject = Messages.get("mail.welcome.subject");
        String message = Messages.get("mail.welcome.message");
        Mail.Envelop envelop = new Mail.Envelop(subject, message, user.email);
        Mail.sendMail(envelop);
    }
}
