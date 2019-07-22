package to.joeli.jass.client.strategy.training

/**
 * By setting a train mode we know where in the training process we are and what environment to set
 */
enum class TrainMode constructor(val isSavingData: Boolean, val isFairTournamentModeEnabled: Boolean) {
    NONE(false, true),
    PRE_TRAIN(true, false),
    SELF_PLAY(true, true),
    EVALUATION(false, true)
}
