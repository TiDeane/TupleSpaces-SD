package pt.ulisboa.tecnico.tuplespaces.server.domain;

public class TupleSpaceObj {

    private String tuple;
    private int flag;
    private int clientId;

    public TupleSpaceObj(String tuple) {
        this.tuple = tuple;
        this.flag = 0;
        this.clientId = -1;
    }

    public String getTuple() {
        return this.tuple;
    }

    public int getFlag() {
        return this.flag;
    }

    public int getClientId() {
        return this.clientId;
    }

    public void lockTuple(int clientId) {
        this.flag = 1;
        this.clientId = clientId;
    }

    public void unlockTuple() {
        this.flag = 0;
        this.clientId = -1;
    }

    public boolean isTupleLocked() {
        return (this.flag == 0);
    }
}