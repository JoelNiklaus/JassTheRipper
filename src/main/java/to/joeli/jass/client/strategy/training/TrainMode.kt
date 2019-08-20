package to.joeli.jass.client.strategy.training

/**
 * By setting a train mode we know where in the training process we are and what environment to set
 */
enum class TrainMode constructor(val isSavingData: Boolean, val isFairTournamentModeEnabled: Boolean) {
    DATA_COLLECTION(true, false),
    EVALUATION(false, true)
}
