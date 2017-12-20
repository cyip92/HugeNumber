public class HugeNumber
{
	///// Fields /////
	double num;
	HugeNumber exp, recurDepth;
	int recurMode;
	
	static HugeNumber zero = new HugeNumber(0);
	static HugeNumber one = new HugeNumber(1);

	static int sciThreshold = 9;								// Switch from full-form number to scientific notation past this many digits
	static int recurDepthThreshold = 5;							// Collapse down the recursion past this depth
	static HugeNumber randomExpThreshold = new HugeNumber(10);	// Give a random exponent past this recursion depth
	static HugeNumber upArrowThreshold = new HugeNumber("1e5");	// Switch to up-arrow notation past this recursion depth

	///// Constructors /////
	public HugeNumber()
	{}

	public HugeNumber(double n)
	{
		if (n == 0)
		{
			num = 0;
		}
		else
		{
			double e = Math.floor(Math.log10(Math.abs(n)));
			exp = new HugeNumber(e);
			num = n / Math.pow(10, e);
		}
		this.standardize();
	}

	public HugeNumber(double n, int e)
	{
		num = n;
		exp = new HugeNumber(e);
		this.standardize();
	}

	// Takes in a floating-point representation as "AAAeBBB", doesn't do input format verification.  Constructs 1 if error occurs.
	public HugeNumber(String fl)
	{
		String[] split = fl.split("e");
		try
		{
			HugeNumber N = new HugeNumber(Double.parseDouble(split[0]), Integer.parseInt(split[1]));
			num = N.num;
			exp = N.exp;
			this.standardize();
		}
		catch (NumberFormatException e)
		{
			num = 1;
			exp = zero;
		}
	}

	public HugeNumber(double n, HugeNumber HN)
	{
		num = n;
		exp = new HugeNumber(HN);
		this.standardize();
	}

	// Properly copy with separate references
	public HugeNumber(HugeNumber N)
	{
		num = N.num;
		if (N.exp == null)
			exp = null;
		else
			exp = new HugeNumber(N.exp);
		recurMode = N.recurMode;
		recurDepth = N.recurDepth;
	}

	///// Methods /////
	public HugeNumber add(HugeNumber N)
	{
		// Things can break if this isn't checked first
		if (this.isZero())
			return N;
		if (N.isZero())
			return this;

		// Makes logic a bit simpler if we assume they have the same recursion properties and N1 > N2
		if (this.recurMode != N.recurMode)
			return max(this, N);
		if (compare(this, N) < 0)
			return N.add(this);

		// Add them
		HugeNumber sum = zero;
		switch (this.recurMode)
		{
		case 0:	// Recursive floating point
			if (Math.abs(this.getDepth() - N.getDepth()) > 1 && this.getDepth() > 2)	// One is vastly larger than the other
				sum = max(this, N);

			else if (this.getDepth() > 4)	// Don't bother unless the exponent stacks are short
				sum = max(this, N);

			else if (this.getDepth() > 1 || N.getDepth() > 1)	// Exponents are close enough that it might matter
			{				
				// Ensure they are both in scientific notation
				HugeNumber sciN1 = this, sciN2 = N;
				while (sciN1.getDepth() >= 2)
					sciN1 = sciN1.collapseTopLevel();
				while (sciN2.getDepth() >= 2)
					sciN2 = sciN2.collapseTopLevel();
				
				// Doesn't do anything if they're too far (double is 15 digits of precision)
				if (Math.abs(Math.rint(sciN1.exp.num - sciN2.exp.num)) > 16)
					return max(this, N);
				
				// Move the decimal point and add
				double n1 = sciN1.num, n2 = sciN2.num;
				int exp1 = (int) Math.rint(sciN1.exp.num), exp2 = (int) Math.rint(sciN2.exp.num);
				if (exp1 > exp2)
					for (; exp2 < exp1; exp2++)
						n2 /= 10;
				else
					for (; exp1 < exp2; exp1++)
						n1 /= 10;
				sum = new HugeNumber(n1 + n2, this.exp);
			}

			else	// They're both small
			{
				sum = new HugeNumber(this.num + N.num, this.exp);
			}

			break;
		case 1:		// Adding two stacked exponents
		default:	// Seriously, don't even bother trying to add these huge numbers...
			sum = max(this, N);	
			break;
		}

		return sum;
	}

	public HugeNumber sub(HugeNumber N)
	{
		// Recursion mode
		if (this.recurMode != N.recurMode)
			return maxAbs(this, N);

		HugeNumber diff = zero;
		switch (this.recurMode)
		{
		case 0:	// Recursive floating point

			if (this.getDepth() != N.getDepth() && this.getDepth() > 2)	// One is vastly larger than the other (10^10^5 >> 10^5)
				diff = maxAbs(this, N);

			else if (this.getDepth() > 2)	// Don't bother unless the exponent stacks are short
				diff = maxAbs(this, N);

			else if (this.getDepth() == 2)	// Exponents are close enough that it might matter
			{
				// Ensure they are both in scientific notation
				HugeNumber sciN1, sciN2;
				if (this.getDepth() == 2)
					sciN1 = this.collapseTopLevel();
				else
					sciN1 = this;
				if (N.getDepth() == 2)
					sciN2 = N.collapseTopLevel();
				else
					sciN2 = N;

				// Don't bother if they're too far (double is 15 digits of precision)
				if (Math.abs(Math.rint(sciN1.exp.num - sciN2.exp.num)) > 16)
					diff = maxAbs(this, N);

				// Move the decimal point and subtract
				double n1 = sciN1.num, n2 = sciN2.num;
				int exp1 = (int) Math.rint(sciN1.exp.num), exp2 = (int) Math.rint(sciN2.exp.num);
				if (exp1 > exp2)
					for (; exp2 < exp1; exp2++)
						n2 /= 10;
				else
					for (; exp1 < exp2; exp1++)
						n1 /= 10;
				diff = new HugeNumber(n1 - n2, this.exp);
			}

			else	// They're both small
				diff = new HugeNumber(this.num - N.num, this.exp);

			break;
		case 1:		// Subtracting two stacked exponents
		default:	// Again, don't even bother trying to subtract these huge numbers...
			diff = maxAbs(this, N);	
			break;
		}

		return diff;
	}

	public HugeNumber multiply(HugeNumber N)
	{
		// Zero?
		if (this.isZero() || N.isZero())
			return zero;

		// Logic becomes simpler if we assume |N1| > |N2|
		if (compare(this.abs(), N.abs()) == -1)
			return N.multiply(this);

		HugeNumber product = new HugeNumber(0);
		switch (this.recurMode)
		{
		case 0:	// Recursive Floating point
			product = new HugeNumber(this.num * N.num, this.exp.add(N.exp));
			break;
		case 1:	// 1st operand: Stacked exponent
			switch (N.recurMode)
			{
			case 0:	// Recursive * Stacked
				product = new HugeNumber(this);	// It's not even close.
				break;
			case 1:	// Stacked * Stacked
				// This is mostly to make it look nice
				HugeNumber bigger = max(this, N);
				bigger.num = this.num * N.num;
				if (bigger.num > 10)
					bigger.num /= 10;
				product = bigger;
				break;
			default:
				System.out.println("HugeNumber is too big to multiply!");
				break;
			}
			break;

		default:
			System.out.println("HugeNumber is too big to multiply!");
		}

		product.standardize();
		return product;
	}

	// Anything bigger than recursive floating point is mapped to zero
	public HugeNumber reciprocal()
	{
		if (this.recurMode != 0)
			return zero;
		
		HugeNumber recip = new HugeNumber(this);
		recip.num = 1 / recip.num;
		recip.exp.num *= -1;
		recip.exp = recip.exp.add(one);
		return recip;
	}
	
	// Evaluates this^N
	public HugeNumber pow(HugeNumber N)
	{
		HugeNumber result = new HugeNumber(0);
		switch (this.recurMode)
		{
		case 0:	// 1st operand: Recursive float
			switch (N.recurMode)
			{
			case 0:	// 2nd operand: Recursive float
				result = (this.log10().multiply(N)).pow10();
				break;
			case 1:	// 2nd operand: Compressed exponents
				result = new HugeNumber(N);
				result.num = this.num;
				result.recurDepth = result.recurDepth.add(one);
				break;
			default:
				System.out.println("HugeNumber (2) is too big to exponentiate!");
			}
			break;

		case 1:	// 1st operand: Compressed exponents
			switch (N.recurMode)
			{
			case 0:	// 2nd operand: Recursive float
				result = new HugeNumber(this);
				result.exp = this.pow(N.exp);
				break;
			case 1:	// 2nd operand: Compressed exponents
				result = new HugeNumber(this);
				result.recurDepth = max(this.recurDepth.add(one), N.recurDepth);
				result.exp = new HugeNumber(N.exp);
				break;
			default:
				System.out.println("HugeNumber (2) is too big to exponentiate!");
			}
			break;

		default:
			System.out.println("HugeNumber (1) is too big to exponentiate!");
		}

		return result;
	}

	// Performs tetration to an integer power
	public HugeNumber tetrate(int N)
	{
		HugeNumber result = this;
		for (int i = 1; i < N; i++)
			result = this.pow(result);
		return result;
	}
	
	public HugeNumber pow10()
	{
		HugeNumber collapsed = this.collapseTopLevel().collapseTopLevel();
		if (collapsed.num < 100 && collapsed.exp == null)	// Assume decimals only really matter with small numbers
			return new HugeNumber(Math.pow(10, collapsed.num));
		else	// Take care of the fractional part of the exponent
			return new HugeNumber(Math.pow(10, collapsed.num - (int) collapsed.num), this);
	}

	public HugeNumber log10()
	{
		return this.exp.add(new HugeNumber(Math.log10(this.num)));
	}

	// Calculates the factorial using Stirling's approximation, N! ~ (N^N)(e^-N)sqrt(2*pi*N) with a correction factor
	public HugeNumber factorial()
	{
		HugeNumber a = this.multiply(new HugeNumber(1 / Math.E)).pow(this);
		HugeNumber b = this.multiply(new HugeNumber(2 * Math.PI)).pow(new HugeNumber(0.5));
		HugeNumber c = one.add(this.multiply(new HugeNumber(12)).reciprocal());
		return a.multiply(b).multiply(c);
	}
	
	public boolean isZero()
	{
		return this.num == 0;
	}

	public boolean isNegative()
	{
		return this.num < 0;
	}

	public static HugeNumber max(HugeNumber N1, HugeNumber N2)
	{
		if (compare(N1, N2) == 1)
			return N1;
		else
			return N2;
	}

	// Returns the number with the larger absolute value
	public static HugeNumber maxAbs(HugeNumber N1, HugeNumber N2)
	{
		if (compare(N1.abs(), N2.abs()) == 1)
			return N1;
		else
			return N2;
	}

	public HugeNumber abs()
	{
		HugeNumber copy = new HugeNumber(this);
		if (copy.num < 0)
			copy.num *= -1;
		return copy;
	}

	// Compares two HugeNumbers, returns 1 if N1 > N2, -1 if N1 < N2, and 0 if N1 = N2, and assumes standardized forms.
	// Generally should not be used for equality due to possible rounding errors.
	public static int compare(HugeNumber N1, HugeNumber N2)
	{
		// Zero?
		if (N1.isZero())
			return N2.isNegative() ? 1 : (N2.isZero() ? 0 : -1);
		if (N2.isZero())
			return N1.isNegative() ? -1 : 1;

		// Check signs
		if (N1.isNegative() && N2.isNegative())
			return compare(N2.abs(), N1.abs());
		else if (N1.isNegative() && !N2.isNegative())
			return -1;
		else if (!N1.isNegative() && N2.isNegative())
			return 1;

		// Recursion beats everything else
		if (N1.recurMode != N2.recurMode)
			if (N1.recurMode > N2.recurMode)
				return 1;
			else
				return -1;

		switch (N1.recurMode)
		{
		case 0:	// Recursive floating point
			int compExp = compare(N1.exp, N2.exp);
			if (compExp != 0)
				return compExp;
			else
			{
				if (N1.num > N2.num)
					return 1;
				else if (N1.num < N2.num)
					return -1;
				else
					return 0;
			}

		case 1:	// Compressed exponents
			if (compare(N1.recurDepth, N2.recurDepth) != 0)		// Recursion depth first
				return compare(N1.recurDepth, N2.recurDepth);
			else if (compare(N1.exp, N2.exp) != 0)		// Exponent next
				return compare(N1.exp, N2.exp);
			else	// Prefix last, reasonable chance of being wrong due to intermediate exponents being lost
			{
				if (N1.num > N2.num)
					return 1;
				else if (N1.num < N2.num)
					return -1;
				else
					return 0;
			}

		default:
			System.out.println("HugeNumber is too big to compare!");
		}
		return -2;
	}

	// Gets the last exponent in a stacked chain
	public HugeNumber getLastExponent()
	{
		if (exp == null)
			return this;
		else
			return exp.getLastExponent();
	}

	// Gets the last exponent in a compressed chain
	public HugeNumber getLastCompressedExponent()
	{
		if (exp.exp.exp.exp == null)
			return this;
		else
			return exp.getLastCompressedExponent();
	}

	// Gets the recursion depth for a stacked exponent
	public int getDepth()
	{
		if (exp == null)
			return 0;
		else
			return exp.getDepth() + 1;
	}

	// Adjusts a HugeNumber if it gets too big (or small?)
	public void standardize()
	{
		if (this.isZero())
			return;

		switch (recurMode)
		{
		case 0:	// Recursive floating point
			if (this.exp == null)
				break;
			while (Math.abs(num) < 1)	// num is too small
			{
				num *= 10;
				exp = exp.sub(one);
			}
			while (Math.abs(num) >= 10)	// num is too big
			{
				num /= 10;
				exp = exp.add(one);
			}
			if (exp != null)	// Do it recursively
				exp.standardize();

			if (getDepth() - 3 > recurDepthThreshold)	// Compress the stack
			{
				recurDepth = new HugeNumber(getDepth() - 4);
				exp = getLastCompressedExponent();
				recurMode = 1;
			}
			break;
		case 1:	// Compressed Exponents

			while (exp.getDepth() > 3)		// exp is too big
			{
				recurDepth = recurDepth.add(one);
				exp = exp.exp;
			}

			break;
		}
	}

	public String toDataString()
	{	
		String str = "[num=" + num + " | rec=" + recurMode + " | exp=";
		if (exp == null)
			return str + "null]";
		else
			return str + "{" + exp.toDataString() + "}]";
	}

	// Collapses the topmost level, doesn't check for possible overflow
	public HugeNumber collapseTopLevel()
	{
		HugeNumber copy = new HugeNumber(this);

		try
		{	
			if (exp.exp == null)
			{
				copy.num = num * Math.pow(10, exp.num);
				copy.exp = null;
			}

			else	// Go deeper
				copy.exp = copy.exp.collapseTopLevel();
		}

		catch (NullPointerException e)	// Not enough levels to collapse!
		{}

		return copy;
	}

	public HugeNumber nonStandardForString()
	{
		HugeNumber copy = new HugeNumber(this);

		try
		{
			if (exp.exp.exp == null && exp.num < sciThreshold)	// Collapse two levels
			{
				copy.num = num * Math.pow(10, exp.num);
				copy.exp = null;

			}

			else if (exp.exp.exp == null)	// Collapse one level
				copy.exp.exp = null;

			else	// Go deeper
				copy.exp = copy.exp.nonStandardForString();
		}

		catch (NullPointerException e)	// Not enough levels to collapse!
		{}

		return copy;
	}

	public String toString()
	{
		switch (recurMode)
		{
		case 0:
			return this.nonStandardForString().toStringNonstandard();
		case 1:
			return this.toStringNonstandard();
		default:
			return "HugeNumber is too big?";
		}
	}

	// Returns a decimal string (3 places) for nice-looking display values, only used for values that don't matter
	private String getRandomLookingDecimal(int seed)
	{
		// Randomize a few times
		int code = this.hashCode();
		for (int i = 0; i < seed; i++)
			code = 3 * (code >> 10) * (code >> 14);

		String value = Math.abs(code) + "999";
		value = value.charAt(0) + "." + value.substring(1, 4);
		return value;
	}

	// Returns a random integer for nice-looking display values, only used for values that don't matter
	private String getRandomLookingInteger(int seed, int digits)
	{
		// Randomize a few times
		int code = this.hashCode();
		for (int i = 0; i < seed; i++)
			code = 3 * (code >> 10) * (code >> 14);

		String value = Math.abs(code) + "9999";
		for (; value.length() > 1 && value.charAt(0) == '0';)
			value = value.substring(1);
		return value.substring(0, digits);
	}
	
	// Returns a String with the specified number of up arrows
	private String upArrows(int n)
	{
		String ret = "";
		for (int i = 0; i < n; i++)
			ret += "^";
		return ret;
	}

	private String toStringNonstandard()
	{
		// Round to 3 decimal places
		String value = num + "00000";
		String str = value.substring(0, 5);

		// Construct a String
		switch (recurMode)
		{
		case 0:	// Recursive floating point
			if (num > 10 || exp == null || exp.isZero())
				return "" + (int) Math.rint(num);
			else if (exp.num > sciThreshold - 0.5 && exp.exp == null)
				return str + "e" + exp.toStringNonstandard();
			else
				return str + "e[" + exp.toStringNonstandard() + "]";
		case 1:	// Compressed exponents
			String compressedPrefix = getRandomLookingDecimal(0) + "e[... " + recurDepth + " ...[";
			if (compare(recurDepth, upArrowThreshold) == 1)		// The exponent doesn't really matter with enough recursion, drop it
				return getRandomLookingDecimal(2) + " * (10 " + upArrows(2) + " " + recurDepth + ")";
			if (compare(recurDepth, randomExpThreshold) == 1)	// Instead of dropping it, give a random one
				return compressedPrefix + getRandomLookingDecimal(1) + "e" + getRandomLookingInteger(2, 4) + "]...]";
			else
				return compressedPrefix + exp + "]...]";
		default:
			return "HugeNumber is too big?";
		}
	}
}
