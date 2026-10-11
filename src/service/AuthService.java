package service;

import model.User;
import util.FileUtil;
import util.InputUtil;

import java.io.IOException;

/** Master-password login. The first run asks the user to create the password. */
public class AuthService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MIN_LENGTH = 4;

    public boolean authenticate() throws IOException {
        if (!FileUtil.exists(FileUtil.PASSWORD_FILE)) {
            return createPassword();
        }
        String stored = FileUtil.readWholeFile(FileUtil.PASSWORD_FILE).trim();
        User owner = new User("owner", stored);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String entered = InputUtil.prompt("Master password: ");
            if (owner.matches(entered)) {
                System.out.println("Welcome back!");
                return true;
            }
            System.out.println("Wrong password (" + attempt + "/" + MAX_ATTEMPTS + ").");
        }
        return false;
    }

    private boolean createPassword() throws IOException {
        System.out.println("First run: create a master password (at least " + MIN_LENGTH + " characters).");
        while (true) {
            String first = InputUtil.prompt("New password: ");
            if (first.length() < MIN_LENGTH) {
                System.out.println("Too short.");
                continue;
            }
            String second = InputUtil.prompt("Repeat password: ");
            if (!first.equals(second)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            User owner = User.fromPlain("owner", first);
            FileUtil.writeText(FileUtil.PASSWORD_FILE, owner.getEncodedPassword());
            System.out.println("Password saved.");
            return true;
        }
    }
}
