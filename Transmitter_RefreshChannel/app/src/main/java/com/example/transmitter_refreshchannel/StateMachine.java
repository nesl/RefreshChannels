package com.example.transmitter_refreshchannel;

public class StateMachine {
    private State currentState;

    public StateMachine(State initialState) {
        this.currentState = initialState;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void handleEvent(boolean isRRCorrectlySet, boolean isDataEnd) {
        switch (currentState) {
            case STATE_IDLE:
                if(isRRCorrectlySet){
                    currentState = State.STATE_SYNC;
                }
                else {
                    currentState = State.STATE_IDLE;
                }
                break;
            case STATE_SYNC:
                if(isRRCorrectlySet){
                    currentState = State.STATE_DATA;
                }
                else {
                    currentState = State.STATE_IDLE;
                }
                break;
            case STATE_DATA:
                if(isRRCorrectlySet){
                    if(isDataEnd){
                        currentState = State.STATE_END;
                    }
                    else {
                        currentState = State.STATE_DATA;
                    }
//                    currentState = State.STATE_END;
                }
                else {
                    currentState = State.STATE_IDLE;
                }
                break;
            case STATE_END:
                currentState = State.STATE_IDLE;
                break;
        }

    }

    public void setState(State state) {
        currentState = state;
    }
}

