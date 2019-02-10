import React from 'react';
import {GameMode} from '../../../shared/game/gameMode';

export default (props) => {

    let mode = props.mode,
        color = props.color,
        cardType = props.cardType,
        imagePath;

    switch (mode) {
        case GameMode.TRUMPF:
            imagePath = '/images/trumpf/' + cardType + '/' + color.toLowerCase() + '.png';
            break;
        case GameMode.OBEABE:
            imagePath = '/images/trumpf/obeabe.jpg';
            break;
        case GameMode.UNDEUFE:
            imagePath = '/images/trumpf/undeufe.jpg';
            break;
        case GameMode.SCHIEBE:
            imagePath = '/images/trumpf/schiebe.jpg';
            break;
    }

    return (
        <img id="trumpf" className={(!mode && !color) ? 'hidden' : ''} src={imagePath}/>
    );
};
