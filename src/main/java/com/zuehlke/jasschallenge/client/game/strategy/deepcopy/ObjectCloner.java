package com.zuehlke.jasschallenge.client.game.strategy.helpers;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Created by joelniklaus on 07.05.17.
 */
public class ObjectCloner {
	// so that nobody can accidentally create an ObjectCloner object
	private ObjectCloner() {
	}

	// returns a deep copy of an object
	static public Object deepCopySerialization(Object oldObj) throws Exception {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			ByteArrayOutputStream bos =
					new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			// serialize and pass the object
			oos.writeObject(oldObj);
			oos.flush();
			ByteArrayInputStream bin =
					new ByteArrayInputStream(bos.toByteArray());
			ois = new ObjectInputStream(bin);
			// return the new object
			return ois.readObject();
		} catch (Exception e) {
			System.out.println("Exception in ObjectCloner = " + e);
			throw (e);
		} finally {
			oos.close();
			ois.close();
		}
	}
}