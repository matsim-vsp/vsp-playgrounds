package playground.gthunig.generalJavaBehavior;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class PassByValuePrincipleTest {

	@Test
	public void testPassByValue() {
		boolean bool = true;
//		bool = getChangedValue(bool);
		changeValue(bool);
		Assert.assertTrue(bool);
//		TODO mit nichtprimitivem Datentyp testen
	}

	@Test
	public void test2() {
		Date d = new Date();
		Date d2 = changeDate(d);
		Assert.assertTrue(d.before(d2));
	}

	private boolean getChangedValue(boolean bool) {
		bool = false;
		return bool;
	}

	private void changeValue(boolean bool) {
		bool = false;
	}

	private Date changeDate(Date d) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		d = new Date();
		return d;
	}

}
