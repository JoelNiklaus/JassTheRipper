import React from 'react';
import ReactCSSTransitionGroup from 'react-addons-css-transition-group';
import {GameState} from './gameStore';
import JassActions from '../jassActions';

let cards = [],
    startingPlayerIndex,
    playerSeating;

export default (props) => {

    if (props.collectStich !== false) {
        cards = props.cards || [];
        startingPlayerIndex = props.startingPlayerIndex;
        playerSeating = props.playerSeating;
    }

    if (props.state === GameState.STICH) {
        setTimeout(() => JassActions.collectStich(), 2000);
    }

    let imagePath = '/images/cards/' + props.cardType + '/';


    const mappedCards = cards.map((card, index) => {
        let actPlayerIndex = (startingPlayerIndex + index) % 4;
        return (
            <img key={card.color + '_' + card.number}
                 className={'card-' + playerSeating[actPlayerIndex]}
                 src={imagePath + card.color.toLowerCase() + '_' + card.number + '.gif'}
            />
        );
    });

    return (
        <div id="tableCards">
            <ReactCSSTransitionGroup
                transitionName="cards"
                transitionEnterTimeout={150}
                transitionLeaveTimeout={500}
            >
                {mappedCards}
            </ReactCSSTransitionGroup>
        </div>
    );
};
