/**
* This file is part of SCAPI.
* SCAPI is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* SCAPI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with SCAPI.  If not, see <http://www.gnu.org/licenses/>.
*
* Any publication and/or code referring to and/or based on SCAPI must contain an appropriate citation to SCAPI, including a reference to http://crypto.cs.biu.ac.il/SCAPI.
*
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
*
*/
/**
* This file is part of SCAPI.
* SCAPI is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* SCAPI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with SCAPI.  If not, see <http://www.gnu.org/licenses/>.
*
* Any publication and/or code referring to and/or based on SCAPI must contain an appropriate citation to SCAPI, including a reference to http://crypto.cs.biu.ac.il/SCAPI.
*
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
*
*/
package edu.biu.scapi.primitives.prg;

import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.exceptions.NoMaxException;
import edu.biu.scapi.primitives.kdf.HKDF;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;
import edu.biu.scapi.primitives.prf.PseudorandomFunction;
import edu.biu.scapi.primitives.prf.bc.BcAES;
import edu.biu.scapi.primitives.prf.bc.BcHMAC;
import edu.biu.scapi.tools.Factories.KdfFactory;
import edu.biu.scapi.tools.Factories.PrfFactory;

public class ScPrgFromPrf implements PseudorandomGenerator{

	private KeyDerivationFunction kdf;	// Underlying KDF.
	private PseudorandomFunction prf;	// Underlying PRF.
	private byte[] ctr;					//Counter used for key generation.
	private boolean isKeySet;
	
	/**
	 * Default constructor. Uses default implementations of KDF and PRF.
	 */
	public ScPrgFromPrf(){
		
		kdf = new HKDF(new BcHMAC());
		prf = new BcAES();
	}
	
	/**
	 * Constructor that lets the user choose the underlying KDF and PRF algorithms.
	 * @param kdf underlying KeyDerivationFunction.
	 * @param prf underlying PseudorandomFunction.
	 */
	public ScPrgFromPrf(KeyDerivationFunction kdf, PseudorandomFunction prf){
		this.kdf = kdf;
		this.prf = prf;
	}
	
	/**
	 * Constructor that lets the user choose the underlying KDF and PRF algorithms.
	 * @param kdfName KeyDerivationFunction algorithm name.
	 * @param prfName PseudorandomFunction algorithm name.
	 */
	public ScPrgFromPrf(String kdfName, String prfName) throws FactoriesException{
		this(KdfFactory.getInstance().getObject(kdfName), PrfFactory.getInstance().getObject(prfName));
	}
	
	/**
	 * Initializes this PRG with SecretKey.
	 * @param secretKey
	 * @throws InvalidKeyException 
	 */
	public void setKey(SecretKey secretKey) throws InvalidKeyException {
		prf.setKey(secretKey); //Sets the key to the underlying prf.
		//Creates the counter. It should be the same size as the prf's block size.
		//If there is no limit on the block size, use default size.
		try {
			ctr = new byte[prf.getBlockSize()];
		} catch (NoMaxException e){
			ctr = new byte[16];
		}
		
		//Initializes the counter to 1.
		ctr[ctr.length-1] = 1;
		isKeySet = true;
		
	}

	@Override
	public boolean isKeySet() {
		return isKeySet;
	}

	/** 
	 * Returns the name of the algorithm - PRG with {name of the underlying prf}.
	 * @return - the algorithm name.
	 */
	@Override
	public String getAlgorithmName() {
		
		return "PRG_from_" + prf.getAlgorithmName();
	}

	/**
	 * Generates a secret key to initialize this prg object.
	 * @param keyParams should be an instance of PrgFromPrfParameterSpec
	 * @return the generated secret key
	 * @throws InvalidParameterSpecException if the given params is not an instance of PrgFromPrfParameterSpec
	 */
	public SecretKey generateKey(AlgorithmParameterSpec keyParams) throws InvalidParameterSpecException {
		if (!(keyParams instanceof PrgFromPrfParameterSpec)){
			throw new IllegalArgumentException("keyParams should be an instance of PrgFromPrfParameterSpec");
		}
		
		//Gets the prg parameters.
		PrgFromPrfParameterSpec params = (PrgFromPrfParameterSpec) keyParams;
		byte[] entropy = params.getEntropySource();
		int prfKeySize = params.getPrfKeySize();
		
		//Uses the KDF to generate a PRF key.
		return kdf.derivateKey(entropy, 0, entropy.length, prfKeySize/8);
	}

	/**
	 * This function is not supported in this implementation. Throws exception.
	 * @throws UnsupportedOperationException 
	 */
	public SecretKey generateKey(int keySize) {
		throw new UnsupportedOperationException("To generate a key for this prg object use the generateKey(AlgorithmParameterSpec keyParams) function");
	}

	/**
	 * Generates pseudorandom bytes using the underlying prf.
	 * @param outBytes - output bytes. The result of streaming the bytes.
	 * @param outOffset - output offset
	 * @param outLen - the required output length.
	 * @throws IllegalStateException if no key was set.
	 * @throws ArrayIndexOutOfBoundsException if the given offset or length is invalid.
	 */
	@Override
	public void getPRGBytes(byte[] outBytes, int outOffset, int outLen) {
		if (!isKeySet()){
			throw new IllegalStateException("secret key isn't set");
		}
		//checks that the offset and the length are correct
		if ((outOffset > outBytes.length) || ((outOffset + outLen) > outBytes.length)){
			throw new ArrayIndexOutOfBoundsException("wrong offset for the given output buffer");
		}
		
		int numGeneratedBytes = 0;	//Number of current generated bytes.
		byte [] generatedBytes = new byte[ctr.length];
		
		while(numGeneratedBytes < outLen){
			try {
				//If the prf can output any length (for example, IteratedPrfVarying) call the computeBlock with the outputLen.
				prf.computeBlock(ctr, 0, ctr.length, outBytes, outOffset + numGeneratedBytes, outLen);
				numGeneratedBytes += outLen;
			} catch (IllegalBlockSizeException e) {
				try {
					//If the prf can receive any input length (for example, Hmac) call the computeBlock with the ctr length.
					//The output is written to a new array because there is no guarantee that output array is long enough to hold the next output block.
					prf.computeBlock(ctr, 0, ctr.length, generatedBytes, 0);
					//Copy the right number of generated bytes.
					if (numGeneratedBytes + generatedBytes.length <= outLen){
						System.arraycopy(generatedBytes, 0, outBytes, outOffset + numGeneratedBytes, generatedBytes.length);
					} else {
						System.arraycopy(generatedBytes, 0, outBytes, outOffset + numGeneratedBytes, outLen - numGeneratedBytes);
					}
					//Increases the number of generated bytes.
					numGeneratedBytes += ctr.length;
				} catch (IllegalBlockSizeException e1) {
					try {
						//If the prf can receive fixed input length (for example, AES) call the computeBlock without the input length.
						//The output is written to a new array because there is no guarantee that output array is long enough to hold the next output block.
						prf.computeBlock(ctr, 0, generatedBytes, 0);
						//Copy the right number of generated bytes.
						if (numGeneratedBytes + generatedBytes.length <= outLen){
							System.arraycopy(generatedBytes, 0, outBytes, outOffset + numGeneratedBytes, generatedBytes.length);
						} else {
							System.arraycopy(generatedBytes, 0, outBytes, outOffset + numGeneratedBytes, outLen - numGeneratedBytes);
						}
						//Increases the number of generated bytes.
						numGeneratedBytes += ctr.length;
					} catch (IllegalBlockSizeException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
			//Increases the counter.
			increaseCtr();
		}
		
	}

	/**
	 * Increases the ctr byte array by 1 bit.
	 */
	private void increaseCtr(){
		
		//increase the counter by one.
		int    carry = 1;
		int len = ctr.length;
		
		for (int i = len - 1; i >= 0; i--)
		{
			int    x = (ctr[i] & 0xff) + carry;

			if (x > 0xff)
			{
				carry = 1;
			}
			else
			{
				carry = 0;
			}

			ctr[i] = (byte)x;
		}
	} 
	
	
}
