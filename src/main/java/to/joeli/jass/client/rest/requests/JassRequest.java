package to.joeli.jass.client.rest.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class JassRequest {
	private final String version;
	@JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = TrumpfFilter.class)
	private final int trump;
	private final int dealer;
	private final int currentPlayer;
	private final int tss;
	private final List<Trick> tricks;
	private final List<Hand> player;
	private final String jassTyp;
	private final String gameId;
	private final int seatId;

	public final String getVersion() {
		return this.version;
	}

	public final int getTrump() {
		return this.trump;
	}

	public final int getDealer() {
		return this.dealer;
	}

	public final int getCurrentPlayer() {
		return this.currentPlayer;
	}

	public final int getTss() {
		return this.tss;
	}

	public final List<Trick> getTricks() {
		return this.tricks;
	}

	public final List<Hand> getPlayer() {
		return this.player;
	}

	public final String getJassTyp() {
		return this.jassTyp;
	}

	public final String getGameId() {
		return this.gameId;
	}

	public final int getSeatId() {
		return this.seatId;
	}

	public JassRequest(String version, int trump, int dealer, int currentPlayer, int tss, List<Trick> tricks, List<Hand> player, String jassTyp, String gameId, int seatId) {
		super();
		this.version = version;
		this.trump = trump;
		this.dealer = dealer;
		this.currentPlayer = currentPlayer;
		this.tss = tss;
		this.tricks = tricks;
		this.player = player;
		this.jassTyp = jassTyp;
		this.gameId = gameId;
		this.seatId = seatId;
	}

	public JassRequest() {
		this("", -1, 0, 0, 0, (List<Trick>) new ArrayList(), (List<Hand>) new ArrayList(), "", "", 0);
	}

	@Override
	public String toString() {
		return "JassRequest{" +
				"version='" + version + '\'' +
				", trump=" + trump +
				", dealer=" + dealer +
				", currentPlayer=" + currentPlayer +
				", tss=" + tss +
				", tricks=" + tricks +
				", player=" + player +
				", jassTyp='" + jassTyp + '\'' +
				", gameId='" + gameId + '\'' +
				", seatId=" + seatId +
				'}';
	}
}
