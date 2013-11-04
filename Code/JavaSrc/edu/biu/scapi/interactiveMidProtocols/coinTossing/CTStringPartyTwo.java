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
package edu.biu.scapi.interactiveMidProtocols.coinTossing;

import java.io.IOException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.exceptions.CommitValueException;
import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.exceptions.InvalidDlogGroupException;
import edu.biu.scapi.exceptions.SecurityLevelException;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtWithProofsReceiver;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtCommitValue;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtRCommitPhaseOutput;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.pedersen.CmtPedersenWithProofsReceiver;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;
import edu.biu.scapi.securityLevel.Malicious;
import edu.biu.scapi.securityLevel.StandAlone;
import edu.biu.scapi.tools.Factories.KdfFactory;

/**
 * Concrete implementation of a protocol for tossing a string from party two's point of view.
 * This protocol is fully secure under the stand-alone simulation-based definitions.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class CTStringPartyTwo implements CTPartyTwo, StandAlone, Malicious{
	
	private Channel channel;
	private CmtWithProofsReceiver receiver;
	private SecureRandom random;
	private int l;
	private KeyDerivationFunction kdf;
	
	/**
	 * Constructor that set the given parameters and creates receiver, ZKPOK verifier and ZK verifier.
	 * @param channel
	 * @param receiver
	 * @param kdf
	 * @param l determining the length of the output
	 * @param random source of randomness
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws SecurityLevelException
	 * @throws InvalidDlogGroupException
	 * @throws ClassNotFoundException
	 * @throws CheatAttemptException
	 */
	public CTStringPartyTwo(Channel channel, CmtWithProofsReceiver receiver,
			KeyDerivationFunction kdf, int l, SecureRandom random) throws IOException, IllegalArgumentException, SecurityLevelException, InvalidDlogGroupException, ClassNotFoundException, CheatAttemptException {
		doConstruct(channel, receiver, kdf, l, random);
	}
	
	
	/**
	 * Default constructor that creates receiver, ZKPOK verifier and ZK verifier.
	 * @param channel
	 * @param l determining the length of the output
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws SecurityLevelException
	 * @throws InvalidDlogGroupException
	 * @throws ClassNotFoundException
	 * @throws CheatAttemptException
	 */
	public CTStringPartyTwo(Channel channel, int l) throws IllegalArgumentException, SecurityLevelException, InvalidDlogGroupException, ClassNotFoundException, IOException, CheatAttemptException  {
		try {
			kdf = KdfFactory.getInstance().getObject("HKDF(HMac(SHA-256))");
		} catch (FactoriesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
			
		doConstruct(channel, new CmtPedersenWithProofsReceiver(channel), kdf, l, new SecureRandom());
	}

	/**
	 * Sets the given parameters and creates receiver, ZKPOK verifier and ZK verifier.
	 * @param channel
	 * @param receiver
	 * @param kdf
	 * @param l 
	 * @param random
	 * @throws SecurityLevelException
	 * @throws InvalidDlogGroupException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws CheatAttemptException
	 */
	private void doConstruct(Channel channel, CmtWithProofsReceiver receiver,
			KeyDerivationFunction kdf, int l, SecureRandom random) throws SecurityLevelException, InvalidDlogGroupException,
					ClassNotFoundException, IOException, CheatAttemptException {
		
		this.receiver = receiver;
		this.kdf = kdf;
		this.channel = channel;
		this.random = random;
		this.l = l;
	}
	
	/**
	 * Execute the following protocol:
	 * "SAMPLE a random L-bit string s2 <- {0,1}^L
	 *	WAIT to receive a COMMIT.commit from P1
	 *	RUN the verifier in a ZKPOK_FROM_SIGMA applied to a SIGMA protocol that P1 knows the committed value. 
	 *	If the verifier output is REJ, then HALT and REPORT ERROR.
	 *	SEND s2 to P1
	 *	WAIT for an L-bit string s1 from P1
	 *	RUN the verifier in ZK_FROM_SIGMA applied to a SIGMA protocol that the committed value was s1. 
	 *	If the verifier output is REJ, then HALT and REPORT ERROR.
	 *	OUTPUT s1 XOR s2".
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws CheatAttemptException 
	 * @throws CommitValueException 
	 */
	public CTOutput toss() throws ClassNotFoundException, IOException, CheatAttemptException, CommitValueException {
		//Sample a random L-bit string s2 <- {0,1}^L
		byte[] s2 = new byte[l/8];
		random.nextBytes(s2);
				
		//Wait to receive a COMMIT.commit from P1.
		CmtRCommitPhaseOutput commitOutput = receiver.receiveCommitment();
		long id = commitOutput.getCommitmentId(); //Get commitment id.
		
		//Run the verifier in a ZKPOK_FROM_SIGMA applied to a SIGMA protocol that P1 knows the committed value
		boolean verified = receiver.verifyKnowledge(id);
		
		//If the verifier output is REJ, then HALT and REPORT ERROR.
		if (!verified){
			throw new CheatAttemptException("The Sigma protocol which proves that P1 knows the committed value was not verified");
		}
		
		//Send s2 to P1
		try {
			channel.send(s2);
		} catch (IOException e) {
			throw new IOException("failed to send the message. The thrown message is: " + e.getMessage());
		}
		
		//Run the verifier in ZK_FROM_SIGMA applied to a SIGMA protocol that the committed value was s1
		CmtCommitValue s1 = receiver.verifyCommittedValue(id);
		
		//If the verifier output is REJ, then HALT and REPORT ERROR.
		if (!verified){
			throw new CheatAttemptException("The Sigma protocol which proves that the committed value was s1 was not verified");
		}
		
		byte[] lBitsS1 = computeKdf(receiver.generateBytesFromCommitValue(s1));
		//Compute s1 XOR s2
		byte[] result = new byte[l/8];
		for (int i=0; i<l/8; i++){
			result[i] = (byte) (lBitsS1[i] ^ s2[i]);
		}
		//Return the output
		return new CTStringOutput(result);
	}
	
	/**
	 * Computes the kdf operation on the given byte array in order to get a L-bit byte array.
	 * @param s1Bytes array to compute the kdf operation on.
	 * @return a L-bit byte array.
	 */
	private byte[] computeKdf(byte[] s1Bytes) {
		//KDF get outLen in bytes. In this case, l/8.
		SecretKey lBitsArray = kdf.deriveKey(s1Bytes, 0, s1Bytes.length, l/8);
		return lBitsArray.getEncoded();
	}
	
}