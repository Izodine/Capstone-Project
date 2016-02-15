package com.syncedsoftware.iassembly.iasm_base.memory;

import java.util.Objects;

public final class Pair<E, F> {
	
	E elementOne;
	F elementTwo;
	
	public Pair(E elementOne, F elementTwo){
		
		if(elementOne == null || elementTwo == null)
			throw new IllegalArgumentException("Constructor arguments cannot be null.");
		
		this.elementOne = elementOne;
		this.elementTwo = elementTwo;
	}
	
	public E first(){
		return elementOne;
	}
	
	public F second(){
		return elementTwo;
	}
	
	@Override
	public int hashCode() {
		return elementOne.hashCode() ^ elementTwo.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Pair)){
			return false;
		}
		Pair<?, ?> sourcePair = (Pair<?, ?>) obj;
	    return Objects.equals(sourcePair.first(), elementOne) && Objects.equals(sourcePair.second(), elementTwo);
	}

}
