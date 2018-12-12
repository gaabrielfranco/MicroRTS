/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.asymmetric.GranularityPGS;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.asymmetric.PGS.PGSmRTSRandom;
import ai.core.AI;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
public class AutomaticTest {
	public static void main(String args[]) throws Exception {
		int wins = 0;
		int losses = 0;
		int draws = 0;

		// List<String> maps = new ArrayList<>(
		// Arrays.asList("maps/8x8/basesWorkers8x8A.xml",
		// "maps/16x16/basesWorkers16x16A.xml",
		// "maps/8x8/FourBasesWorkers8x8.xml", "maps/16x16/TwoBasesBarracks16x16.xml"));

		List<String> maps = new ArrayList<>(Arrays.asList("maps/24x24/basesWorkers24x24A.xml"));
		List<String> IAs = new ArrayList<>(Arrays.asList("gPGS", "gPGS3", "gPGS4", "gPGS5", "PGS"));
		int NUM_EXEC = 20;

		for (String map : maps) {
			for (int j = 0; j < IAs.size() - 1; j++) {
				for (int k = j + 1; k < IAs.size(); k++) {
					FileWriter arq = new FileWriter("/home/gabriel/Desktop/gPGS/" + IAs.get(j) + " vs " + IAs.get(k)
							+ " " + map.split("/")[2].split("x")[0]);
					PrintWriter gravarArq = new PrintWriter(arq);
					gravarArq.printf("---------AI's---------%n");
					gravarArq.printf("AI 0 = " + IAs.get(j) + "%n");
					gravarArq.printf("AI 1 = " + IAs.get(k) + "%n");
					gravarArq.printf("Mapa = " + map + "%n%n");
					System.out.println("---------AI's---------");
					System.out.println("AI 0 = " + IAs.get(j));
					System.out.println("AI 1 = " + IAs.get(k) + "\n");

					for (int i = 0; i < NUM_EXEC; i++) {
						UnitTypeTable utt = new UnitTypeTable();
						PhysicalGameState pgs = PhysicalGameState.load(map, utt);

						GameState gs = new GameState(pgs, utt);
						int MAXCYCLES = 6000;
						int PERIOD = 20;
						boolean gameover = false;
						AI ai1 = null;
						AI ai2 = null;

						switch (IAs.get(j)) {
						case "gPGS":
							ai1 = new GranularityPGSRandom(utt);
							break;
						case "gPGS3":
							ai1 = new PGSRandomBaseline(utt, 3);
							break;
						case "gPGS4":
							ai1 = new PGSRandomBaseline(utt, 4);
							break;
						case "gPGS5":
							ai1 = new PGSRandomBaseline(utt, 5);
							break;
						case "PGS":
							ai1 = new PGSmRTSRandom(utt);
							break;
						}

						switch (IAs.get(k)) {
						case "gPGS":
							ai2 = new GranularityPGSRandom(utt);
							break;
						case "gPGS3":
							ai2 = new PGSRandomBaseline(utt, 3);
							break;
						case "gPGS4":
							ai2 = new PGSRandomBaseline(utt, 4);
							break;
						case "gPGS5":
							ai2 = new PGSRandomBaseline(utt, 5);
							break;
						case "PGS":
							ai2 = new PGSmRTSRandom(utt);
							break;
						}

						System.out.println("Iteração " + (i + 1));
						gravarArq.printf("Iteração " + (i + 1) + "%n");

						long startTime = System.currentTimeMillis();
						long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
						do {
							if (System.currentTimeMillis() >= nextTimeToUpdate) {
								startTime = System.currentTimeMillis();
								PlayerAction pa1 = ai1.getAction(0, gs);

								startTime = System.currentTimeMillis();
								PlayerAction pa2 = ai2.getAction(1, gs);

								gs.issueSafe(pa1);
								gs.issueSafe(pa2);

								// simulate:
								gameover = gs.cycle();
								nextTimeToUpdate += PERIOD;
							} else {
								try {
									Thread.sleep(1);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} while (!gameover && gs.getTime() < MAXCYCLES);
						System.out.println("Winner " + Integer.toString(gs.winner()));
						System.out.println("Game Over\n");
						gravarArq.printf("Winner " + Integer.toString(gs.winner()) + "%n");
						gravarArq.printf("Game Over%n%n");
						if (gs.winner() == 0) {
							wins++;
						} else if (gs.winner() == 1) {
							losses++;
						} else {
							draws++;
						}
					}
					System.out.println("V/E/D = " + wins + "/" + draws + "/" + losses);
					gravarArq.printf("V/E/D = " + wins + "/" + draws + "/" + losses);
					gravarArq.close();
					wins = 0;
					losses = 0;
					draws = 0;
				}
			}
		}
	}
}
