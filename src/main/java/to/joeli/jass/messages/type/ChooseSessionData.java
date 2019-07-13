package to.joeli.jass.messages.type;

public class ChooseSessionData {
    private final SessionChoice sessionChoice;
    private final String sessionName;
    private final SessionType sessionType;
    private final Boolean asSpectator = false;
    private final String advisedPlayerName;
    private final Integer chosenTeamIndex;

    public ChooseSessionData(SessionChoice sessionChoice, String sessionName, SessionType sessionType, Integer chosenTeamIndex, String advisedPlayerName) {
        this.sessionChoice = sessionChoice;
        this.sessionName = sessionName;
        this.sessionType = sessionType;
        this.chosenTeamIndex = chosenTeamIndex;
        this.advisedPlayerName = advisedPlayerName;
    }

    public SessionChoice getSessionChoice() {
        return sessionChoice;
    }

    public String getSessionName() {
        return sessionName;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public Boolean isAsSpectator() {
        return asSpectator;
    }

    public String getAdvisedPlayerName() {
        return advisedPlayerName;
    }

    public Integer getChosenTeamIndex() {
        return chosenTeamIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChooseSessionData that = (ChooseSessionData) o;

        if (sessionChoice != that.sessionChoice) return false;
        return !(sessionName != null ? !sessionName.equals(that.sessionName) : that.sessionName != null);

    }

    @Override
    public int hashCode() {
        int result = sessionChoice != null ? sessionChoice.hashCode() : 0;
        result = 31 * result + (sessionName != null ? sessionName.hashCode() : 0);
        return result;
    }
}
