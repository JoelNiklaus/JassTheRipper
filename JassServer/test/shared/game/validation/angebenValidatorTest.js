import assert from 'assert';
import * as Card from '../../../../shared/deck/card';
import { CardColor } from '../../../../shared/deck/cardColor';
import AngebenValidator from '../../../../shared/game/validation/angebenValidator';
import { GameMode } from '../../../../shared/game/gameMode';


describe('Angeben Validator', function () {


    it('should allow any card, if no cards have been played', () => {
        let parameters = {
            color: CardColor.CLUBS,
            mode: GameMode.TRUMPF,
            tableCards: [],
            handCards: [Card.create(6, CardColor.HEARTS), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(6, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should allow any Trumpf', () => {
        let parameters = {
            color: CardColor.HEARTS,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(10, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should not respect trumpfColor when OBEABE', () => {
        let parameters = {
            color: CardColor.HEARTS,
            mode: GameMode.OBEABE,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(10, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(!validationResult.permitted);
    });

    it('should NOT allow any color, if a player still has a card of the correct color', () => {
        let parameters = {
            color: CardColor.SPADES,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(10, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(!validationResult.permitted);
    });

    it('should allow the same color', () => {
        let parameters = {
            color: CardColor.SPADES,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(10, CardColor.DIAMONDS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should allow to hold the trumpf buur', () => {
        let parameters = {
            color: CardColor.SPADES,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.SPADES)],
            handCards: [Card.create(11, CardColor.SPADES), Card.create(10, CardColor.DIAMONDS)],
            cardToPlay: Card.create(10, CardColor.DIAMONDS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should not allow to "nicht angeben" trumpf when more than buur', () => {
        let parameters = {
            color: CardColor.SPADES,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.SPADES)],
            handCards: [Card.create(11, CardColor.SPADES), Card.create(10, CardColor.DIAMONDS), Card.create(10, CardColor.SPADES)],
            cardToPlay: Card.create(10, CardColor.DIAMONDS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(!validationResult.permitted);
    });

    it('should allow another color, if no cards of the given color have been left', () => {
        let parameters = {
            color: CardColor.SPADES,
            mode: GameMode.TRUMPF,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(11, CardColor.HEARTS), Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.HEARTS)],
            cardToPlay: Card.create(10, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should allow card if obenabä', () => {
        let parameters = {
            mode: GameMode.OBEABE,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(11, CardColor.SPADES), Card.create(10, CardColor.HEARTS), Card.create(10, CardColor.HEARTS)],
            cardToPlay: Card.create(10, CardColor.HEARTS)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(validationResult.permitted);
    });

    it('should deny card if undäufä and could angeben', () => {
        let parameters = {
            mode: GameMode.UNDEUFE,
            tableCards: [Card.create(6, CardColor.DIAMONDS)],
            handCards: [Card.create(11, CardColor.SPADES), Card.create(10, CardColor.DIAMONDS), Card.create(10, CardColor.HEARTS)],
            cardToPlay: Card.create(11, CardColor.SPADES)
        };

        let validationResult = AngebenValidator.validate(parameters);

        assert(!validationResult.permitted);
    });
});

