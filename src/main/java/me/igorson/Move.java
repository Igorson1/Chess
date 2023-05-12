package me.igorson;

public class Move {

    public Move(int from, int to){
        this.from = from;
        this.to = to;
        pieceToPromote = 0;
    }

    public Move(int from, int to, int pieceToPromote){
        this.from = from;
        this.to = to;
        this.pieceToPromote = pieceToPromote;
    }
    int from, to, pieceToPromote;

    @Override
    public String toString(){
        return from + " " + to;
    }

}
