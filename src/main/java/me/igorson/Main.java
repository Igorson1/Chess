package me.igorson;

import org.apache.batik.transcoder.TranscoderException;

import java.io.IOException;

public class Main{


    public static void main(String[] args) throws IOException, InterruptedException, TranscoderException {

        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

        Game game = new Game(fen, Piece.White, true, true, true ,true);
        game.start();


    }
}