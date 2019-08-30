package to.joeli.jass.client.rest

import to.joeli.jass.game.mode.Mode

data class TrumpResponse(val trump: Int) {
    constructor() : this(0) // This constructor is needed for JSON serialization to work correctly
    constructor(mode: Mode) : this(mode.code)
}
