package to.joeli.jass.client.rest.requests;

public class TrumpfFilter {
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Integer)) {
			return false;
		}
		// trumpf should not be equal to default (-1)
		int trumpf = (int) obj;
		return trumpf == -1;
	}
}
