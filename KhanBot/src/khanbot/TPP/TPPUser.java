package khanbot.TPP;

import java.io.Serializable;

public class TPPUser implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4635116000168275540L;
	String userName;
	int bets;
	int wins;
	int balance;
	
	public TPPUser(String userName)
	{
		this.userName = userName;
		bets = 0;
		wins = 0;
		balance = -1;
	}
	
	public String toString()
	{
		return userName + ": " + wins + "/" + bets;
	}
	
}
