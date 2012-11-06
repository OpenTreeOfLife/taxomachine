package opentree.utils;

public final class Levenshtein {
    
    public Levenshtein() {}
    
    public static int distance(String a, String b)
    {
        // Minimize the amount of storage needed:
        if (a.length() > b.length())
        {
            // Swap:
            String x = a;
            a = b;
            b = x;
        }

        // Store only two rows of the matrix, instead of a big one
        int[] mat1 = new int[a.length() + 1];
        int[] mat2 = new int[a.length() + 1];

        int i;
        int j;

        for (i = 1; i <= a.length(); i++)
            mat1[i] = i;

        mat2[0] = 1;

        for (j = 1; j <= b.length(); j++)
        {
            for (i = 1; i <= a.length(); i++)
            {
                int c = (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);

                mat2[i] =
                    Math.min(mat1[i - 1] + c,
                    Math.min(mat1[i] + 1, mat2[i - 1] + 1));
            }

            // Swap:
            int[] x = mat1;
            mat1 = mat2;
            mat2 = x;

            mat2[0] = mat1[0] + 1;
        }

        // It's row #1 because we swap rows at the end of each outer loop,
        // as we are to return the last number on the lowest row
        return mat1[a.length()];
    }
}
