package com.jcloisterzone.game.expansion;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Maps;
import com.jcloisterzone.Player;
import com.jcloisterzone.action.BridgeAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.EdgePattern;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.collection.Sites;
import com.jcloisterzone.game.ExpandedGame;

public class BridgesCastlesBazaarsGame extends ExpandedGame {
	
	private boolean bridgeUsed;
	private Map<Player, Integer> bridges = Maps.newHashMap();
	private Map<Player, Integer> castles = Maps.newHashMap();
	
	@Override
	public void initPlayer(Player player) {		
		int players = game.getAllPlayers().length;
		if (players < 5) {
			bridges.put(player, 3);
			castles.put(player, 3);
		} else {
			bridges.put(player, 2);
			castles.put(player, 2);
		}
	}
	
	@Override
	public void initTile(Tile tile, Element xml) {
		if (xml.getElementsByTagName("bazaar").getLength() > 0) {
			tile.setTrigger(TileTrigger.BAZAAR);
		}		
	}
	
	@Override
	public void turnCleanUp() {
		bridgeUsed = false;
	}
	
	@Override
	public void prepareActions(List<PlayerAction> actions, Sites commonSites) {
		if (! bridgeUsed && getPlayerBridges(game.getPhase().getActivePlayer()) > 0) {
			BridgeAction action = prepareBridgeAction();
			if (action != null) {
				actions.add(action);
			}
		} 
	}
	
	public BridgeAction prepareMandatoryBridgeAction() {
		Tile tile = game.getTile();	
		for(Entry<Location, Tile> entry : getBoard().getAdjacentTilesMap(tile.getPosition()).entrySet()) {
			Tile adjacent = entry.getValue();
			Location rel = entry.getKey();
			
			char adjacentSide = adjacent.getEdge(rel.rev());
			char tileSide = tile.getEdge(rel);
			if (tileSide != adjacentSide) {
				Location bridgeLoc = getBridgeLocationForAdjacent(rel);
				BridgeAction action = prepareTileBridgeAction(tile, null, bridgeLoc);
				if (action != null) return action;
				return prepareTileBridgeAction(adjacent, null, bridgeLoc);
			}			
		}
		throw new IllegalStateException();
	}
	
	private Location getBridgeLocationForAdjacent(Location rel) {
		if (rel == Location.N || rel == Location.S) {
			return Location.NS;
		} else {
			return Location.WE;
		}	
	}
	
	private BridgeAction prepareBridgeAction() {
		BridgeAction action = null;		
		Tile tile = game.getTile();		
		action = prepareTileBridgeAction(tile, action, Location.NS);
		action = prepareTileBridgeAction(tile, action, Location.WE);
		for(Entry<Location, Tile> entry : getBoard().getAdjacentTilesMap(tile.getPosition()).entrySet()) {
			Tile adjacent = entry.getValue();
			Location rel = entry.getKey();
			action = prepareTileBridgeAction(adjacent, action, getBridgeLocationForAdjacent(rel));
		}
		return action;
	}
		
	private BridgeAction prepareTileBridgeAction(Tile tile, BridgeAction action, Location bridgeLoc) {
		if (isBridgePlacementAllowed(tile, tile.getPosition(), bridgeLoc)) {
			if (action == null) action = new BridgeAction();
			action.getSites().getOrCreate(tile.getPosition()).add(bridgeLoc);
		}
		return action;
	}
	
	private boolean isBridgePlacementAllowed(Tile tile, Position p, Location bridgeLoc) {
		if (! tile.isBridgeAllowed(bridgeLoc)) return false;
		for (Entry<Location, Tile> e : getBoard().getAdjacentTilesMap(p).entrySet()) {			
			Location rel = e.getKey();
			if (rel.intersect(bridgeLoc) != null) {
				Tile adjacent = e.getValue();
				char adjacentSide = adjacent.getEdge(rel.rev()); 
				if (adjacentSide != 'R') return false;				
			}
		}
		return true;
	}
	
	@Override
	public boolean isSpecialPlacementAllowed(Tile tile, Position p) {
		if (getPlayerBridges(game.getActivePlayer()) > 0) {			
			if (isTilePlacementWithBridgeAllowed(tile, p, Location.NS)) return true;
			if (isTilePlacementWithBridgeAllowed(tile, p, Location.WE)) return true;			
			if (isTilePlacementWithOneAdjacentBridgeAllowed(tile, p)) return true;
		}
		return false;
	}
		
	private boolean isTilePlacementWithBridgeAllowed(Tile tile, Position p, Location bridgeLoc) {
		if (! tile.isBridgeAllowed(bridgeLoc)) return false;

		for (Entry<Location, Tile> e : getBoard().getAdjacentTilesMap(p).entrySet()) {						
			Tile adjacent = e.getValue();
			Location rel = e.getKey();			
			
			char adjacentSide = adjacent.getEdge(rel.rev());
			char tileSide = tile.getEdge(rel);
			if (rel.intersect(bridgeLoc) != null) {
				if (adjacentSide != 'R') return false;				
			} else {
				if (adjacentSide != tileSide) return false;
			}
		}		
		return true;
	}
	
	private boolean isTilePlacementWithOneAdjacentBridgeAllowed(Tile tile, Position p) {
		boolean bridgeUsed = false;
		for (Entry<Location, Tile> e : getBoard().getAdjacentTilesMap(p).entrySet()) {
			Tile adjacent = e.getValue();
			Location rel = e.getKey();			
			
			char tileSide = tile.getEdge(rel);
			char adjacentSide = adjacent.getEdge(rel.rev());
			
			if (tileSide != adjacentSide) {
				if (bridgeUsed) return false;
				if (tileSide != 'R') return false;
				
				Location bridgeLoc = getBridgeLocationForAdjacent(rel);							
				if (! isBridgePlacementAllowed(adjacent, adjacent.getPosition(), bridgeLoc)) return false;
				bridgeUsed = true;
			}
		}
		return bridgeUsed; //ok if exactly one bridge is used 
	}
	
	public int getPlayerCastles(Player pl) {
		return castles.get(pl);
	}
	
	public int getPlayerBridges(Player pl) {
		return bridges.get(pl);
	}
	
	public void decreaseCastles(Player player) {
		int n = getPlayerCastles(player);
		if (n == 0) throw new IllegalStateException("Player has no castles");
		castles.put(player, n-1);
	}
	
	public void decreaseBridges(Player player) {
		int n = getPlayerBridges(player);
		if (n == 0) throw new IllegalStateException("Player has no bridges");
		bridges.put(player, n-1);
	}
	
	public void deployBridge(Position pos, Location loc) {
		Tile tile = getBoard().get(pos);
		if (! tile.isBridgeAllowed(loc)) {
			throw new IllegalArgumentException("Cannot deploy " + loc + " bridge on " + pos);
		}
		bridgeUsed = true;
		tile.placeBridge(loc);
		game.fireGameEvent().bridgeDeployed(pos, loc);
	}
	
	@Override
	public BridgesCastlesBazaarsGame copy() {
		BridgesCastlesBazaarsGame copy = new BridgesCastlesBazaarsGame();
		copy.game = game;
		copy.bridges = Maps.newHashMap(bridges);
		copy.castles = Maps.newHashMap(castles);
		return copy;
	}	
	
	@Override
	public void saveToSnapshot(Document doc, Element node) {	
		node.setAttribute("bridgeUsed", bridgeUsed + "");
		for(Player player: game.getAllPlayers()) {
			Element el = doc.createElement("player");
			node.appendChild(el);
			el.setAttribute("index", "" + player.getIndex());
			el.setAttribute("castles", "" + getPlayerCastles(player));
			el.setAttribute("bridges", "" + getPlayerBridges(player));			
		}
	}
	
	@Override
	public void loadFromSnapshot(Document doc, Element node) {		
		bridgeUsed = Boolean.parseBoolean(node.getAttribute("bridgeUsed"));
		NodeList nl = node.getElementsByTagName("player");
		for(int i = 0; i < nl.getLength(); i++) {
			Element playerEl = (Element) nl.item(i);
			Player player = game.getPlayer(Integer.parseInt(playerEl.getAttribute("index")));
			castles.put(player, Integer.parseInt(playerEl.getAttribute("castles")));			
			bridges.put(player, Integer.parseInt(playerEl.getAttribute("bridges")));
		}
	}
}