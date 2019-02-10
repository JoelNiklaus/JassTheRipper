import React from 'react';
import ExistingSessions from './existingSessions.jsx';
import {GameSetupState} from './gameSetupStore';
import JassActions from '../jassActions';
import {SessionType} from '../../../shared/session/sessionType';

function createNewSession(sessionType, asSpectator, event) {
    let inputElement = event.target,
        sessionName = inputElement.value;

    if (event.charCode === 13 && sessionName.trim()) {
        inputElement.disabled = true;
        JassActions.createNewSession(sessionType, sessionName, asSpectator);
    }
}

export default (props) => {

    let status = props.setupState.status;

    return (
        <div id="chooseSession" className={(status !== GameSetupState.CHOOSE_SESSION ? 'hidden' : '')}>
            <h1 className="jumbotron">Choose Session</h1>
            <ExistingSessions sessions={props.setupState.sessions}/>
            <div className="session-choice">
                <input type="text" name="createNewSession" placeholder="Session Name..."
                       onKeyPress={(event) => createNewSession(SessionType.SINGLE_GAME, false, event)}
                />
            </div>
            <div className="session-choice">
                <input type="text" name="createNewTournament" placeholder="Tournament Name..."
                       onKeyPress={(event) => createNewSession(SessionType.TOURNAMENT, true, event)}
                />
            </div>
            <div className="session-choice">
                <button type="button" name="autoJoin" onClick={JassActions.autojoinSession}>Just Join!</button>
            </div>
        </div>
    );
};
