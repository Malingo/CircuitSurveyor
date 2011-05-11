
/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 *
 * The code for this class comes from Rosetta Code (http://rosettacode.org/wiki/Reduced_row_echelon_form),
 * where it is released to the public under the terms of the GNU Free Documentation License, version 1.3.
 */

/**
 * @author Rosetta Code.
 * {@link http://rosettacode.org/wiki/Reduced_row_echelon_form}
 */

public class RowReducer {

	public static void rref(double[][] matrix) {
		int rowCount = matrix.length;
		if (rowCount == 0)
			return;

		int columnCount = matrix[0].length;

		int lead = 0;
		for (int r = 0; r < rowCount; r++) {
			if (lead >= columnCount)
				break;
			{
				int i = r;
				while (matrix[i][lead] == 0) {
					i++;
					if (i == rowCount) {
						i = r;
						lead++;
						if (lead == columnCount)
							return;
					}
				}
				double[] temp = matrix[r];
				matrix[r] = matrix[i];
				matrix[i] = temp;
			}

			{
				double lv = matrix[r][lead];
				for (int j = 0; j < columnCount; j++)
					matrix[r][j] /= lv;
			}

			for (int i = 0; i < rowCount; i++) {
				if (i != r) {
					double lv = matrix[i][lead];
					for (int j = 0; j < columnCount; j++)
						matrix[i][j] -= lv * matrix[r][j];
				}
			}
			lead++;
		}
	}
}