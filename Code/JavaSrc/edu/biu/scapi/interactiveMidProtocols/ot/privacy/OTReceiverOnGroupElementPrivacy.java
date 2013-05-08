/**
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
* Copyright (c) 2012 - SCAPI (http://crypto.biu.ac.il/scapi)
* This file is part of the SCAPI project.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
* to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
* FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* We request that any publication and/or code referring to and/or based on SCAPI contain an appropriate citation to SCAPI, including a reference to
* http://crypto.biu.ac.il/SCAPI.
* 
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
*/
package edu.biu.scapi.interactiveMidProtocols.ot.privacy;

import java.math.BigInteger;
import java.security.SecureRandom;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.interactiveMidProtocols.ot.OTROnGroupElementOutput;
import edu.biu.scapi.interactiveMidProtocols.ot.OTROutput;
import edu.biu.scapi.interactiveMidProtocols.ot.OTSMessage;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;

/**
 * Concrete class for OT Privacy assuming DDH receiver ON GROUP ELEMENT.
 * This class derived from OTReceiverDDHPrivacyAbs and implements the functionality 
 * related to the byte array inputs.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class OTReceiverOnGroupElementPrivacy extends OTReceiverDDHPrivacyAbs{

	private GroupElement c0, c1;
	/**
	 * Constructor that gets the channel and choose default values of DlogGroup and SecureRandom.
	 */
	public OTReceiverOnGroupElementPrivacy(Channel channel){
		super(channel);
	}
	
	/**
	 * Constructor that sets the given channel, dlogGroup and random.
	 * @param channel
	 * @param dlog must be DDH secure.
	 * @param random
	 */
	public OTReceiverOnGroupElementPrivacy(Channel channel, DlogGroup dlog, SecureRandom random){
		
		super(channel, dlog, random);
	}

	/**
	 * Run the following line from the protocol:
	 * "IF  NOT 
	 *		•	w0, w1, c0, c1 in the DlogGroup
	 *	REPORT ERROR"
	 * @param message received from the sender. must be OTSOnGroupElementPrivacyMessage.
	 * @throws CheatAttemptException if there was a cheat attempt during the execution of the protocol.
	 */
	protected void checkReceivedTuple(OTSMessage message) throws CheatAttemptException{
		//If message is not instance of OTSOnGroupElementPrivacyMessage, throw Exception.
		if(!(message instanceof OTSOnGroupElementPrivacyMessage)){
			throw new IllegalArgumentException("message should be instance of OTSOnGroupElementPrivacyMessage");
		}
		
		OTSOnGroupElementPrivacyMessage msg = (OTSOnGroupElementPrivacyMessage)message;
		
		//Reconstruct the group elements from the given message.
		w0 = dlog.reconstructElement(true, msg.getW0());
		w1 = dlog.reconstructElement(true, msg.getW1());
		c0 = dlog.reconstructElement(true, msg.getC0());
		c1 = dlog.reconstructElement(true, msg.getC1());
		
		
		if (!(dlog.isMember(w0))){
			throw new CheatAttemptException("w0 element is not a member in the current DlogGroup");
		}
		if (!(dlog.isMember(w1))){
			throw new CheatAttemptException("w1 element is not a member in the current DlogGroup");
		}
		if (!(dlog.isMember(c0))){
			throw new CheatAttemptException("c0 element is not a member in the current DlogGroup");
		}
		if (!(dlog.isMember(c1))){
			throw new CheatAttemptException("c1 element is not a member in the current DlogGroup");
		}
		
	}

	/**
	 * Run the following lines from the protocol:
	 * "COMPUTE (kσ)^(-1) = (wσ)^(-beta)
	 *	OUTPUT  xσ = cσ * (kσ)^(-1)"
	 * @return OTROutput contains Xֿƒ
	 */
	protected OTROutput computeFinalXSigma() {
		
		GroupElement kSigma = null;
		GroupElement cSigma = null;
		BigInteger minusBeta = dlog.getOrder().subtract(beta);
		
		//If σ = 0, compute w0^beta and set cσ to c0.
		if (sigma == 0){
			kSigma = dlog.exponentiate(w0, minusBeta);
			cSigma = c0;
		} 
		
		//If σ = 0, compute w1^beta and set cσ to c1.
		if (sigma == 1) {
			kSigma = dlog.exponentiate(w1, minusBeta);
			cSigma = c1;
		}
		
		GroupElement xSigma = dlog.multiplyGroupElements(cSigma, kSigma);
		
		//Create and return the output containing xσ
		return new OTROnGroupElementOutput(xSigma);
	}

}
