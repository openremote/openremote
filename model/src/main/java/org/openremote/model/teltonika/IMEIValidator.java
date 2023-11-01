package org.openremote.model.teltonika;

/**
 * Class that validates an IMEI value.
 * Employs checksum as outlined by the GSM Association.
 * Class retrieved from <a href="https://www.geeksforgeeks.org/program-check-valid-imei-number/">GeeksForGeeks</a>.
 *
 * @see <a href="https://www.gsma.com/newsroom/wp-content/uploads//TS.06-v16.0.pdf">the GSMA IMEI Allocation Guidelines</a>.
 *
 */
public class IMEIValidator {
    // Function for finding and returning
    // sum of digits of a number
    static int sumDig(int n) {
        int a = 0;
        while (n > 0) {
            a = a + n % 10;
            n = n / 10;
        }
        return a;
    }

    public static boolean isValidIMEI(long n) {
        // Converting the number into String
        // for finding length
        String s = Long.toString(n);
        int len = s.length();

        if (len != 15)
            return false;

        int sum = 0;
        for (int i = len; i >= 1; i--) {
            int d = (int) (n % 10);

            // Doubling every alternate digit
            if (i % 2 == 0)
                d = 2 * d;

            // Finding sum of the digits
            sum += sumDig(d);
            n = n / 10;
        }

        return (sum % 10 == 0);
    }
}

