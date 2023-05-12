package me.igorson;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ChessBoardWithColumnsAndRows {
    private JButton[] chessBoardSquares = new JButton[64];
    private JPanel chessBoard = new JPanel(new GridLayout(9, 9));
    private static final String COLS = "ABCDEFGH";

    private Game game;

    JFrame f;

    ChessBoardWithColumnsAndRows(Game game) {
        this.game = game;

        Runnable r = () -> {

            f = new JFrame("Chess");

            reloadGui();

            f.add(chessBoard);

            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setLocationByPlatform(true);

            f.pack();
            f.setMinimumSize(f.getSize());
            f.setResizable(false);
            f.setVisible(true);
        };

        SwingUtilities.invokeLater(r);

    }

    public final void reloadGui() {

                chessBoard.removeAll();

                Insets buttonMargin = new Insets(0, 0, 0, 0);
                for (int i = 0; i < 64; i++) {

                    JButton b = new JButton();
                    b.setMargin(buttonMargin);
                    b.setSize(new Dimension(100, 100));

                    if (getPieceURI(game.getSquares()[i]) != null) {
                        ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();

                        TranscoderInput transcoderInput = new TranscoderInput(String.valueOf(getClass().getClassLoader().getResource(getPieceURI(game.getSquares()[i]))));
                        TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

                        try {
                            PNGTranscoder pngTranscoder = new PNGTranscoder();
                            pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, 64f);
                            pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 64f);
                            pngTranscoder.transcode(transcoderInput, transcoderOutput);

                            resultByteStream.flush();

                            ImageIcon image = new ImageIcon(ImageIO.read(new ByteArrayInputStream(resultByteStream.toByteArray())));

                            b.setIcon(image);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    int row = i / 8 + 1;
                    int file = ((i + 8) % 8) + 1;

                    if (row % 2 == 0) {
                        if (file % 2 != 0) {
                            b.setBackground(Color.decode("#A68973"));
                        } else {
                            b.setBackground(Color.decode("#FFEEE2"));

                        }
                    } else {
                        if (file % 2 != 0) {
                            b.setBackground(Color.decode("#FFEEE2"));

                        } else {
                            b.setBackground(Color.decode("#A68973"));

                        }
                    }

                    b.setBorderPainted(false);
                    b.setFocusPainted(false);
                    chessBoardSquares[i] = b;
                }


                chessBoard.add(new JLabel(""));
                for (int i = 0; i < 8; i++) {

                    if(game.currentMove == Piece.Black){
                        chessBoard.add(
                                new JLabel(COLS.substring(8 - i - 1, 8 - i),
                                        SwingConstants.CENTER));

                    } else {
                        chessBoard.add(
                                new JLabel(COLS.substring(i, i + 1),
                                        SwingConstants.CENTER));
                    }
                }
                for (int i = 0; i < 64; i++) {

                    switch ((i + 8) % 8) {
                        case 0:

                            if(game.currentMove == Piece.Black){
                                chessBoard.add(new JLabel("" + ((i / 8) + 1),
                                        SwingConstants.CENTER));
                            } else {
                                chessBoard.add(new JLabel("" + (8 - (i / 8)),
                                        SwingConstants.CENTER));
                            }

                        default:
                            if(game.currentMove == Piece.Black){
                                chessBoard.add(chessBoardSquares[63 - i]);
                            } else {
                                chessBoard.add(chessBoardSquares[i]);
                            }
                    }
                }

                f.revalidate();
    }

    private String getPieceURI(int piece) {
        String result = null;

        switch (piece - game.getColor(piece)) {
            case 1:
                result = game.getColor(piece) == Piece.White ? "wPawn.svg" : "bPawn.svg";
                break;
            case 2:
                result = game.getColor(piece) == Piece.White ? "wBishop.svg" : "bBishop.svg";
                break;
            case 3:
                result = game.getColor(piece) == Piece.White ? "wKnight.svg" : "bKnight.svg";
                break;
            case 4:
                result = game.getColor(piece) == Piece.White ? "wRook.svg" : "bRook.svg";
                break;
            case 5:
                result = game.getColor(piece) == Piece.White ? "wQueen.svg" : "bQueen.svg";
                break;
            case 6:
                result = game.getColor(piece) == Piece.White ? "wKing.svg" : "bKing.svg";
                break;

        }
        return result;
    }

}