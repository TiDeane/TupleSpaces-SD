package pt.ulisboa.tecnico.tuplespaces.server.domain;

public class TakeObj implements Comparable<TakeObj>{
    Thread thread;
    int seqNumber;

    public TakeObj(int seqNumber) {
        this.thread = Thread.currentThread();
        this.seqNumber = seqNumber;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public int compareTo(TakeObj other) {
        return Integer.compare(this.seqNumber, other.seqNumber);
    }

    @Override
    public String toString() {
        return "Thread: " + thread + ", SeqNumber: " + seqNumber;
    }
}