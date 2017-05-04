import {expect} from 'chai';
import sinon from 'sinon';
import React from 'react';
import JassActions from '../../../client/js/jassActions';
import ExistingSessions from '../../../client/js/gameSetup/existingSessions.jsx';
import {SessionType} from '../../../shared/session/sessionType';

import TestUtils from 'react-addons-test-utils';

import ChooseSession from '../../../client/js/gameSetup/chooseSession.jsx';
import {GameSetupState} from '../../../client/js/gameSetup/gameSetupStore';

describe('ChooseSession Component', () => {

    const shallowRenderer = TestUtils.createRenderer();
    
    let createNewSessionSpy; 
    
    beforeEach(() => {
        createNewSessionSpy = sinon.spy(JassActions, 'createNewSession');
    });

    afterEach(() => {
        JassActions.createNewSession.restore();
    });

    it('should render a div element with id chooseSession and class hidden', () => {
        shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CONNECTING }}));
        let actual = shallowRenderer.getRenderOutput();

        expect(actual.type).to.equal('div');
        expect(actual.props.id).to.equal('chooseSession');
        expect(actual.props.className).to.equal('hidden');
    });

    it('should remove class hidden when GameState CHOOSE_SESSION', () => {
        shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CHOOSE_SESSION }}));
        let actual = shallowRenderer.getRenderOutput();

        expect(actual.type).to.equal('div');
        expect(actual.props.id).to.equal('chooseSession');
        expect(actual.props.className).to.equal('');
    });

    it('should have the right children', () => {
        shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CHOOSE_SESSION }}));
        let actual = shallowRenderer.getRenderOutput();

        let children = actual.props.children;
        expect(children[0].type).to.equal('h1');
        expect(children[1].type).to.equal(ExistingSessions);
        expect(children[2].type).to.equal('div');
        expect(children[2].props.className).to.equal('session-choice');
        expect(children[3].type).to.equal('div');
        expect(children[3].props.className).to.equal('session-choice');
        expect(children[4].type).to.equal('div');
        expect(children[4].props.className).to.equal('session-choice');
    });

    it('should pass the sessions of GameSetupState to ExistingSessions', () => {
        let sessions = ['sessionName'];

        shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CHOOSE_SESSION, sessions }}));
        let actual = shallowRenderer.getRenderOutput();

        let children = actual.props.children;
        expect(children[1].props.sessions).to.equal(sessions);
    });

    it('should add event listeners to children', () => {
        shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CHOOSE_SESSION }}));
        let actual = shallowRenderer.getRenderOutput();

        let newSessionInput = actual.props.children[2].props.children;
        newSessionInput.props.onKeyPress({target:{value:'testSingle'}, charCode:13});
        sinon.assert.calledWith(createNewSessionSpy, SessionType.SINGLE_GAME, 'testSingle', false);

        let newTournamentInput = actual.props.children[3].props.children;       
        newTournamentInput.props.onKeyPress({target:{value:'testTournament'}, charCode:13});
        sinon.assert.calledWith(createNewSessionSpy, SessionType.TOURNAMENT, 'testTournament', true);

        let autojoinInput = actual.props.children[4].props.children;
        expect(autojoinInput.props.onClick).to.equal(JassActions.autojoinSession);
    });

    describe('createNewSession', () => {
        let eventDummy = {
                target: {
                    value: ''
                }
            };

        let createNewSession;

        beforeEach(() => {
            shallowRenderer.render(React.createElement(ChooseSession, { setupState: { status: GameSetupState.CHOOSE_SESSION }}));
            createNewSession = shallowRenderer.getRenderOutput().props.children[2].props.children.props.onKeyPress;
        });

        it('should not start action with keypress which is not Enter', () => {
            eventDummy.charCode = 99;

            createNewSession(eventDummy);
            sinon.assert.callCount(createNewSessionSpy, 0);
        });


        it('should not start action with empty username', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = '';

            createNewSession(eventDummy);

            sinon.assert.callCount(createNewSessionSpy, 0);
        });

        it('should not start action with whitespace username', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = '   ';

            createNewSession(eventDummy);

            sinon.assert.callCount(createNewSessionSpy, 0);
        });

        it('should start action and disable input with valid playername and enter key pressed', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = 'sessionName';

            createNewSession(eventDummy);

            sinon.assert.calledWith(createNewSessionSpy, SessionType.SINGLE_GAME, eventDummy.target.value);
            sinon.assert.callCount(createNewSessionSpy, 1);

            expect(eventDummy.target.disabled).to.equal(true);
        });
    });

});