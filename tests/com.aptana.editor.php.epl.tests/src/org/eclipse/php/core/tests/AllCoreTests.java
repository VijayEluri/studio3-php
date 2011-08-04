package org.eclipse.php.core.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.php.core.tests.dom_ast.parser.DomParserTests;
import org.eclipse.php.core.tests.markoccurrence.MarkOccurrenceTests;

import com.aptana.editor.php.formatter.tests.FormattingTests;

public class AllCoreTests
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllCoreTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(DomParserTests.suite());
		suite.addTest(MarkOccurrenceTests.suite());
		suite.addTest(new FormattingTests().suite());
		// $JUnit-END$
		return suite;
	}
}
