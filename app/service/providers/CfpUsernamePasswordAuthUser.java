package service.providers;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.NameIdentity;
import models.utils.AppException;
import models.utils.Hash;
import service.providers.CfpUsernamePasswordAuthProvider.MySignup;

public class CfpUsernamePasswordAuthUser extends UsernamePasswordAuthUser
        implements NameIdentity {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String name;

    public CfpUsernamePasswordAuthUser(final MySignup signup) {
        super(signup.inputPassword, signup.email);
        this.name = signup.fullname;
    }

    /**
     * Used for password reset only - do not use this to signup a user!
     * @param password
     */
    public CfpUsernamePasswordAuthUser(final String password) {
        super(password, null);
        name = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected String createPassword(String clearString) {
        try {
            return Hash.createPassword(clearString);
        } catch (AppException e) {

        }
        return null;
    }
}