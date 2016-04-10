package com.tinyrye.android.util;

import java.util.ArrayList;
import java.util.List;

public class TieredComparison
{
	private int shortCircuitValue = 0;

	public <C extends Comparable<C>> TieredComparison append(final C v1, final C v2) {
		final int result = v1.compareTo(v2);
		if (shortCircuitValue == 0 && result != 0) shortCircuitValue = result;
		return this;
	}

	public int get() { return shortCircuitValue; }
}