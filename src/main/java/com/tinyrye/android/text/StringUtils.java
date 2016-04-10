package com.tinyrye.android.text;

import java.util.Iterator;

public class StringUtils
{
	public static String join(Iterable<String> sequence, String delimiter)
	{
		Iterator<String> sequenceItr = sequence.iterator();
		if (sequenceItr.hasNext())
		{
			StringBuilder build = new StringBuilder(sequenceItr.next());
			while (sequenceItr.hasNext()) {
				build.append(delimiter).append(sequenceItr.next());
			}
			return build.toString();
		}
		else {
			return "";
		}
	}
}