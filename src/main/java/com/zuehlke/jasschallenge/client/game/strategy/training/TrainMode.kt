package com.zuehlke.jasschallenge.client.game.strategy.training

/**
 * By setting a train mode we know where in the training process we are and what environment to set
 */
enum class TrainMode constructor(val path: String, val isSavingData: Boolean, val isFairTournamentModeEnabled: Boolean) {
    NONE("", false, true),
    PRE_TRAIN("pre_train/", true, false),
    SELF_PLAY("self_play/", true, true),
    EVALUATION("evaluation/", false, true)
}
