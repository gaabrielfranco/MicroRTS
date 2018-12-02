/*
  * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.asymmetric.GranularityPGS;

import ai.RandomBiasedAI;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.partialobservability.POHeavyRushV2;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.POLightRushV2;
import ai.abstraction.partialobservability.PORangedRush;
import ai.abstraction.partialobservability.PORangedRushV2;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.abstraction.partialobservability.POWorkerRushV2;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.asymmetric.common.UnitScriptData;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.InterruptibleAI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;


import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import util.Pair;
import java.util.Comparator;

//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;


/**
 *
 * @author Gabriel Franco and rubens
 */
public class GranularityPGSLimit extends AIWithComputationBudget implements InterruptibleAI{
    
    int LOOKAHEAD = 200;
    int I = 1;  // number of iterations for improving a given player
    int R = 0;  // number of times to improve with respect to the response fo the other player
    EvaluationFunction evaluation = null;
    List<AI> scripts = null;
    UnitTypeTable utt;
    PathFinding pf;
    int _startTime;
    
    HashMap<Long,Set<UnitAction>> actionsAnalyzed = null;
    
    public AI defaultScript = null;
    private AI enemyScript = null;

    long start_time = 0;
    int nplayouts = 0;
    
    GameState gs_to_start_from = null;
    int playerForThisComputation;
    double _bestScore;
    
    AI randAI = null;
    int qtdSumPlayout = 2;

    public GranularityPGSLimit(UnitTypeTable utt) {
        this(100, -1, 200, 1, 1, 
             new SimpleSqrtEvaluationFunction3(),
             //new SimpleSqrtEvaluationFunction2(),
             //new LanchesterEvaluationFunction(),
             utt,
             new AStarPathFinding());
    }
    
    public GranularityPGSLimit(int time, int max_playouts, int la, int a_I, int a_R, EvaluationFunction e, UnitTypeTable a_utt, PathFinding a_pf) {
        super(time, max_playouts);
        
        LOOKAHEAD = la;
        I = a_I;
        R = a_R;
        evaluation = e;
        utt = a_utt;
        pf = a_pf;
        defaultScript = new POLightRush(a_utt);
        scripts = new ArrayList<>();
        buildPortfolio(); 
        randAI = new RandomBiasedAI(utt);
        actionsAnalyzed = new HashMap<Long,Set<UnitAction>>();
    }
    
    protected void buildPortfolio(){
        this.scripts.add(new POLightRush(utt));
        this.scripts.add(new POHeavyRush(utt));
        this.scripts.add(new PORangedRush(utt));
        this.scripts.add(new POWorkerRush(utt));
        
        //this.scripts.add(new POHeavyRush(utt, new FloodFillPathFinding()));
        //this.scripts.add(new POLightRush(utt, new FloodFillPathFinding()));
        //this.scripts.add(new PORangedRush(utt, new FloodFillPathFinding()));
        
    }
    
    
    @Override
    public void reset() {
        
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
    	scripts.clear();
    	buildPortfolio();
       if (gs.canExecuteAnyAction(player)) {
            startNewComputation(player,gs);
            return getBestActionSoFar();
        } else {
            return new PlayerAction();        
        }  
        
    }
    
    @Override
    public PlayerAction getBestActionSoFar() throws Exception {

        //pego o melhor script do portfolio para ser a semente
        AI seedPlayer = getSeedPlayer(playerForThisComputation);
        AI seedEnemy = getSeedPlayer(1-playerForThisComputation);
        
        enemyScript = seedEnemy;
        defaultScript = seedPlayer;
        
        UnitScriptData currentScriptData = new UnitScriptData(playerForThisComputation);
        currentScriptData.setSeedUnits(seedPlayer);
        setAllScripts(playerForThisComputation, currentScriptData, seedPlayer);
        if( (System.currentTimeMillis()-start_time ) < TIME_BUDGET){
            currentScriptData = doPortfolioSearch(playerForThisComputation, currentScriptData, seedEnemy);
        }
        
        return getFinalAction(currentScriptData);
    }
    
    public UnitScriptData getUnitScript(int player, GameState gs) throws Exception {
       
            startNewComputation(player,gs);
            return getBestUnitScriptSoFar();
        
    }
    
    public UnitScriptData continueImproveUnitScript(int player, GameState gs, UnitScriptData currentScriptData) throws Exception {
        startNewComputation(player,gs);
        
        //pego o melhor script do portfolio para ser a semente
        AI seedPlayer = getSeedPlayer(playerForThisComputation);
        AI seedEnemy = getSeedPlayer(1-playerForThisComputation);
        
        defaultScript = seedPlayer;
        enemyScript = seedEnemy;
        
        currentScriptData.setSeedUnits(seedPlayer);
        
        if( (System.currentTimeMillis()-start_time ) < TIME_BUDGET){
           currentScriptData = doPortfolioSearch(playerForThisComputation, currentScriptData, seedEnemy);
        }
        
        return currentScriptData;
    }
    
    public UnitScriptData getBestUnitScriptSoFar() throws Exception {

        //pego o melhor script do portfolio para ser a semente
        AI seedPlayer = getSeedPlayer(playerForThisComputation);
        AI seedEnemy = getSeedPlayer(1-playerForThisComputation);
        
        defaultScript = seedPlayer;
        enemyScript = seedEnemy;
        
        UnitScriptData currentScriptData = new UnitScriptData(playerForThisComputation);
        currentScriptData.setSeedUnits(seedPlayer);
        setAllScripts(playerForThisComputation, currentScriptData, seedPlayer);
        if( (System.currentTimeMillis()-start_time ) < TIME_BUDGET){
           currentScriptData = doPortfolioSearch(playerForThisComputation, currentScriptData, seedEnemy);
        }
        
        return currentScriptData;
    }
    
    
    protected AI getSeedPlayer(int player) throws Exception{
        AI seed = null;
        double bestEval = -9999;
        AI enemyAI = new POLightRush(utt);
        //vou iterar para todos os scripts do portfolio
        for (AI script : scripts) {
            double tEval = eval(player, gs_to_start_from, script, enemyAI);
            if(tEval > bestEval){
                bestEval = tEval;
                seed = script;
            }
        }
        
        return seed;
    }

    /*
    * Executa um playout de tamanho igual ao @LOOKAHEAD e retorna o valor
    */
    public double eval(int player, GameState gs, AI aiPlayer, AI aiEnemy) throws Exception{
        AI ai1 = aiPlayer.clone();
        AI ai2 = aiEnemy.clone();
        
        GameState gs2 = gs.clone();
        ai1.reset();
        ai2.reset();
        int timeLimit = gs2.getTime() + LOOKAHEAD;
        boolean gameover = false;
        while(!gameover && gs2.getTime()<timeLimit) {
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {
                gs2.issue(ai1.getAction(player, gs2));
                gs2.issue(ai2.getAction(1-player, gs2));
            }
        }        
        double e = evaluation.evaluate(player, 1-player, gs2);

        return e;
    }
    
    /**
     * Realiza um playout (Dave playout) para calcular o improve baseado nos scripts existentes.
     * @param player
     * @param gs
     * @param uScriptPlayer
     * @param aiEnemy
     * @return a avaliação para ser utilizada como base. 
     * @throws Exception 
     */
    public double eval(int player, GameState gs, UnitScriptData uScriptPlayer, AI aiEnemy) throws Exception{
        AI ai2 = aiEnemy.clone();
        ai2.reset();
        GameState gs2 = gs.clone();
        
        PlayerAction pAction = uScriptPlayer.getAction(player, gs2);
        List<Pair<Unit, UnitAction>> unitActions = pAction.getActions();
        for(Pair<Unit, UnitAction> p: unitActions)
        {
        	if(!actionsAnalyzed.containsKey(p.m_a.getID()))
        	{
        		Set<UnitAction> set = new HashSet<UnitAction>();
        		set.add(p.m_b);
        		actionsAnalyzed.put(p.m_a.getID(), set);
        	}
        	else
        	{
        		actionsAnalyzed.get(p.m_a.getID()).add(p.m_b);
        	}
        }
        //gs2.issue(uScriptPlayer.getAction(player, gs2));
        gs2.issue(pAction);
        gs2.issue(ai2.getAction(1 - player, gs2));
        int timeLimit = gs2.getTime() + LOOKAHEAD;
        boolean gameover = false;
        while (!gameover && gs2.getTime() < timeLimit) {
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {
                gs2.issue(uScriptPlayer.getAction(player, gs2));
				gs2.issue(ai2.getAction(1-player, gs2));
            }
        }
        
        return evaluation.evaluate(player, 1-player, gs2);
    }
    
    @Override
    public AI clone() {
        return new GranularityPGSLimit(TIME_BUDGET, ITERATIONS_BUDGET, LOOKAHEAD, I, R, evaluation, utt, pf);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("I", int.class, 1));
        parameters.add(new ParameterSpecification("R", int.class, 1));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        
        return parameters;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + LOOKAHEAD + ", " + I + ", " + R + ", " + evaluation + ", " + pf + ")";
    }
    
    public int getPlayoutLookahead() {
        return LOOKAHEAD;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        LOOKAHEAD = a_pola;
    }

    
    public int getI() {
        return I;
    }
    
    
    public void setI(int a) {
        I = a;
    }
    
    
    public int getR() {
        return R;
    }
    
    
    public void setR(int a) {
        R = a;
    }
       
    
    public EvaluationFunction getEvaluationFunction() {
        return evaluation;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        evaluation = a_ef;
    }        
        
    
    public PathFinding getPathFinding() {
        return pf;
    }
    
    
    public void setPathFinding(PathFinding a_pf) {
        pf = a_pf;
    }    

    public double getBestScore() {
        return _bestScore;
    }

    public AI getDefaultScript() {
        return defaultScript;
    }

    public AI getEnemyScript() {
        return enemyScript;
    }
    

    @Override
    public void startNewComputation(int player, GameState gs) throws Exception {
        playerForThisComputation = player;
        gs_to_start_from = gs;
        nplayouts = 0;
        _startTime = gs.getTime();
        start_time = System.currentTimeMillis();
        _bestScore = 0.0;
    }

    @Override
    public void computeDuringOneGameFrame() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void setAllScripts(int playerForThisComputation, UnitScriptData currentScriptData, AI seedPlayer) {
        currentScriptData.reset();
        for(Unit u : gs_to_start_from.getUnits()) {
            if(u.getPlayer() == playerForThisComputation){
                currentScriptData.setUnitScript(u, seedPlayer);
            }
        }
    }
    

	public class UnitComparatorReverse implements Comparator<Unit> {
		public int compare(Unit arg0, Unit arg1) {
			
			double DPSunit0 = ((double) arg0.getMaxDamage() / (double) arg0.getAttackTime()) / arg0.getHitPoints();
			double DPSunit1 = ((double) arg1.getMaxDamage() / (double) arg1.getAttackTime()) / arg1.getHitPoints();
			
			if (DPSunit0 < DPSunit1)
			{
				return 1;
			}
			else if (DPSunit0 > DPSunit1) 
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}

    private UnitScriptData doPortfolioSearch(int player, UnitScriptData currentScriptData, AI seedEnemy) throws Exception {
    	int enemy = 1-player;
        
        UnitScriptData bestScriptData = currentScriptData.clone();
        double bestScore = eval(player, gs_to_start_from, bestScriptData, seedEnemy);
        ArrayList<Unit> unitsPlayer = getUnitsPlayer(player);
        
        int counterIterations = 0;
        //Controle pelo número de iterações
        for (int i = 0; i < I; i++) {
            //fazer o improve de cada unidade
            for (Unit unit : unitsPlayer) {
            	
                //inserir controle de tempo
                if(System.currentTimeMillis() >= (start_time + (TIME_BUDGET-10))  ){
                    return currentScriptData;
                }
                //iterar sobre cada script do portfolio
                for(AI ai : scripts){
                    currentScriptData.setUnitScript(unit, ai);
                    double sum = 0.0;
                    for (int j = 0; j < qtdSumPlayout; j++) {
                        sum += eval(player, gs_to_start_from, currentScriptData, seedEnemy);
                    }
                    double scoreTemp = sum / qtdSumPlayout;
                    
                    if(scoreTemp > bestScore){
                        bestScriptData = currentScriptData.clone();
                        bestScore = scoreTemp;
                    }
                    if( (counterIterations == 0 && scripts.get(0)==ai) || scoreTemp > _bestScore  ){
                        _bestScore = bestScore;
                    }
                }
                //seto o melhor vetor para ser usado em futuras simulações
                currentScriptData = bestScriptData.clone();
            }
            
        	// Controle de tempo
        	if(System.currentTimeMillis() >= (start_time + (TIME_BUDGET-10))  ){
                return currentScriptData;
            }
   
        	// Ordeno as unidades pelo critério que quero usar pra melhorar (ordem decrescente)
        	unitsPlayer.sort(new UnitComparatorReverse());
        	
        	// Map das ações disponíveis para cada unidade
        	HashMap<Long, List<UnitAction>> unitActionsMap = new HashMap<Long, List<UnitAction>>();
        	int iterations = 0;
        	Boolean allEmpty = false;
        	
        	while (System.currentTimeMillis() < (start_time + (TIME_BUDGET - 8))) {
        		// Pega a unidade que vai ser melhorada e as ações dela
        		Unit unitImprove = unitsPlayer.get(iterations);
        		
        		// Se as ações possíveis estão no map de ações possíveis, pego elas, senão, chama getUnitActions
        		List<UnitAction> actions = null;
        		Boolean containKey = false;
    			if(unitActionsMap.containsKey(unitImprove.getID())) 
    			{
    				actions = unitActionsMap.get(unitImprove.getID());
    				containKey = true;
    			}
    			else
    			{
    				actions = unitImprove.getUnitActions(gs_to_start_from);
    			}
    			
    			// Remove as ações possíveis que já foram buscadas pelo PGS
    			if (actionsAnalyzed.containsKey(unitImprove.getID()))
    			{
            		for (int it = 0; it < actions.size(); it++)
            		{
            			if(actionsAnalyzed.get(unitImprove.getID()).contains(actions.get(it)))
            			{
            				actions.remove(i);
            			}
            		}
    			}
    			
    			// Se as ações possíveis não estão no map, elas são colocadas
    			if(!containKey)
    			{
    				unitActionsMap.put(unitImprove.getID(), actions);
    			}

        		// Se existem ações que não foram buscadas pelo PGS
    			if (actions.size() != 0)
    			{
    				// Cria um script que retorna uma ação aleatória nessa game state
    				int randomPos = ThreadLocalRandom.current().nextInt(0, actions.size());
    				AI currentScript = currentScriptData.getAIUnit(unitImprove);
    				AI newScript = null;
    				
    				// Escolhendo o novo script baseado no script atual do jogador
    				if (currentScript.toString().equals("POLightRush(AStarPathFinding)"))
    				{
    					newScript = new POLightRushV2(utt, gs_to_start_from, unitImprove, actions.get(randomPos));
    				}
    				else if(currentScript.toString().equals("POHeavyRush(AStarPathFinding)"))
    				{
    					newScript = new POHeavyRushV2(utt, gs_to_start_from, unitImprove, actions.get(randomPos));
    				}
    				else if(currentScript.toString().equals("PORangedRush(AStarPathFinding)"))
    				{
    					newScript = new PORangedRushV2(utt, gs_to_start_from, unitImprove, actions.get(randomPos));
    				}
    				else
    				{
    					newScript = new POWorkerRushV2(utt, gs_to_start_from, unitImprove, actions.get(randomPos));
    				}
	            	
	            	// Seto o novo script que vai ser testado
	            	UnitScriptData newScriptData = currentScriptData.clone();
	            	newScriptData.setUnitScript(unitImprove, newScript);
	            	
	            	// Avalio esse novo script
	            	double sum = 0.0;
	                for (int j = 0; j < qtdSumPlayout; j++) {
	                    sum += eval(player, gs_to_start_from, newScriptData, seedEnemy);
	                }
	                double scoreTemp = sum / qtdSumPlayout;
	                
	            	if (scoreTemp > _bestScore)
	            	{
	            		currentScriptData = newScriptData.clone();
	            		scripts.add(newScript);
	            		_bestScore = scoreTemp;
	            	}
	            	
	            	// Coloco a ação que acabou de ser analisada no map de ações já buscadas
	            	if(!actionsAnalyzed.containsKey(unitImprove.getID()))
	            	{
	            		Set<UnitAction> set = new HashSet<UnitAction>();
	            		set.add(actions.get(randomPos));
	            		actionsAnalyzed.put(unitImprove.getID(), set);
	            	}
	            	else
	            	{
	            		actionsAnalyzed.get(unitImprove.getID()).add(actions.get(randomPos));
	            	}
    			}
    			else
    			{
    				//Verifico se existem mais ações possíveis. Se não existe, a busca acaba
    				Set<Long> keys = unitActionsMap.keySet();
    				allEmpty = true;
    				for(Long k: keys)
    				{
    					if(!unitActionsMap.get(k).isEmpty())
    					{
    						allEmpty = false;
    						break;
    					}
    				}
    			}
    			
    			if (allEmpty)
    			{
    				return currentScriptData;
    			}
    			iterations = (iterations + 1) % unitsPlayer.size();
        	}
            counterIterations++;
        }
        return currentScriptData;
    }

    private ArrayList<Unit> getUnitsPlayer(int player) {
        ArrayList<Unit> unitsPlayer = new ArrayList<>();
        for(Unit u : gs_to_start_from.getUnits()){
            if(u.getPlayer() == player){
                unitsPlayer.add(u);
            }
        }
        
        return unitsPlayer;
    }

    public PlayerAction getFinalAction(UnitScriptData currentScriptData) throws Exception {
        PlayerAction pAction = new PlayerAction();
        HashMap<String, PlayerAction> actions = new HashMap<>();
        for (AI script : scripts) {
            actions.put(script.toString(), script.getAction(playerForThisComputation, gs_to_start_from));
        }
        for(Unit u : currentScriptData.getUnits()){
            AI ai = currentScriptData.getAIUnit(u);
            
            UnitAction unt = actions.get(ai.toString()).getAction(u);
            if(unt != null){
                pAction.addUnitAction(u, unt);
            }
        }
        
        return pAction;
    }
    
}
