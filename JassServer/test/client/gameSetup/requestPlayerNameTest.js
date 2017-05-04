import {expect} from 'chai';
import sinon from 'sinon';
import React from 'react';
import {GameSetupState} from '../../../client/js/gameSetup/gameSetupStore';
import JassActions from '../../../client/js/jassActions';

import TestUtils from 'react-addons-test-utils';

import RequestPlayerName from '../../../client/js/gameSetup/requestPlayerName.jsx';

describe('RequestPlayerName Component', () => {

    const shallowRenderer = TestUtils.createRenderer();

    it('should render a div element with id requestPlayerName and class hidden', () => {
        shallowRenderer.render(React.createElement(RequestPlayerName, { setupState: GameSetupState.CONNECTING }));
        let actual = shallowRenderer.getRenderOutput();

        expect(actual.type).to.equal('div');
        expect(actual.props.id).to.equal('requestPlayerName');
        expect(actual.props.className).to.equal('hidden');
    });

    it('should remove class hidden when setupState SET_PLAYER_NAME', () => {
        shallowRenderer.render(React.createElement(RequestPlayerName, { setupState: GameSetupState.SET_PLAYER_NAME }));
        let actual = shallowRenderer.getRenderOutput();

        expect(actual.props.className).to.equal('');
    });

    it('should add function onKeyPress to input', () => {
        shallowRenderer.render(React.createElement(RequestPlayerName, { setupState: GameSetupState.SET_PLAYER_NAME }));
        let actual = shallowRenderer.getRenderOutput();

        let input = actual.props.children;
        expect(input.type).to.equal('input');
        expect(input.props.onKeyPress).to.be.a('function');
    });

    describe('choosePlayerName', () => {
        let eventDummy = {
                target: {
                    value: ''
                }
            },
            choosePlayerNameSpy;

        let choosePlayerName;

        beforeEach(() => {
            shallowRenderer.render(React.createElement(RequestPlayerName, { setupState: GameSetupState.SET_PLAYER_NAME }));
            choosePlayerName = shallowRenderer.getRenderOutput().props.children.props.onKeyPress;
            choosePlayerNameSpy = sinon.stub(JassActions, 'choosePlayerName');

        });

        afterEach(() => {
            JassActions.choosePlayerName.restore();
        });

        it('should not start action with keypress which is not Enter', () => {
            eventDummy.charCode = 99;

            choosePlayerName(eventDummy);

            sinon.assert.callCount(choosePlayerNameSpy, 0);
        });


        it('should not start action with empty username', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = '';

            choosePlayerName(eventDummy);

            sinon.assert.callCount(choosePlayerNameSpy, 0);
        });

        it('should not start action with whitespace username', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = '   ';

            choosePlayerName(eventDummy);

            sinon.assert.callCount(choosePlayerNameSpy, 0);
        });

        it('should start action and disable input with valid playername and enter key pressed', () => {
            eventDummy.charCode = 13;
            eventDummy.target.value = 'playerName';

            choosePlayerName(eventDummy);

            sinon.assert.calledWith(choosePlayerNameSpy, eventDummy.target.value);
            sinon.assert.callCount(choosePlayerNameSpy, 1);

            expect(eventDummy.target.disabled).to.equal(true);
        });
    });

});