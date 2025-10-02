package com.example.englishforum.core.common

import com.example.englishforum.core.model.VoteState

fun resolveVoteChange(currentState: VoteState, targetState: VoteState): Pair<VoteState, Int> {
    val nextState = if (currentState == targetState) {
        VoteState.NONE
    } else {
        targetState
    }

    val delta = when (currentState to nextState) {
        VoteState.NONE to VoteState.UPVOTED -> 1
        VoteState.NONE to VoteState.DOWNVOTED -> -1
        VoteState.UPVOTED to VoteState.NONE -> -1
        VoteState.DOWNVOTED to VoteState.NONE -> 1
        VoteState.UPVOTED to VoteState.DOWNVOTED -> -2
        VoteState.DOWNVOTED to VoteState.UPVOTED -> 2
        else -> 0
    }

    return nextState to delta
}
