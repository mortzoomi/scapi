/**
 * 
 */
package edu.biu.scapi.primitives.kdf.bc;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.generators.BaseKDFBytesGenerator;
import org.bouncycastle.crypto.generators.KDF1BytesGenerator;
import org.bouncycastle.crypto.params.ISO18033KDFParameters;
import org.bouncycastle.crypto.params.KDFParameters;

import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.exceptions.UnInitializedException;
import edu.biu.scapi.primitives.hash.TargetCollisionResistant;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;
import edu.biu.scapi.tools.Factories.BCFactory;

/** 
 * @author LabTest
*/
public class BcKdfISO18033 implements KeyDerivationFunction {

	BaseKDFBytesGenerator bcKdfGenerator;
	boolean isInitialized = false;
	
	/**
	 * create the related bc kdf
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws FactoriesException 
	 */
	public BcKdfISO18033(String hash) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FactoriesException {
		
		//pass a digest to the KDF.
		bcKdfGenerator = new KDF1BytesGenerator(BCFactory.getInstance().getDigest(hash));
		
	}
	
	/**
	 * create the related bc kdf. Retrieve the related Digest out of the hash name 
	 * @param hash - the underlying collision resistant hash
	 * @throws FactoriesException 
	 */
	public BcKdfISO18033(TargetCollisionResistant hash) throws FactoriesException {
		//first check that the hmac is initialized.
		if(hash.isInitialized()){
			//pass a digest to the KDF.
			bcKdfGenerator = new KDF1BytesGenerator(BCFactory.getInstance().getDigest(hash.getAlgorithmName()));
		}
		else{//the user must pass an initialized object, otherwise throw an exception
			throw new IllegalStateException("argumrnt hmac must be initialized");
		}
	}
	
	/**
	 * Should not be called. There is not key for this class.
	 */
	public void init(SecretKey secretKey) {
		isInitialized = true;
		
	}


	/**
	 * Should not be called. There is not key for this class.
	 */
	public void init(SecretKey secretKey, AlgorithmParameterSpec params) {
		isInitialized = true;
		
	}


	/**
	 * 
	 */
	public boolean isInitialized() {
		
		return isInitialized;
	}
	
	public SecretKey generateKey(SecretKey key, int len) throws UnInitializedException {
		
		return generateKey(key, len, null);
	}

	/**
	 * @throws UnInitializedException 
	 * 
	 */
	public SecretKey generateKey(SecretKey key, int outLen, byte[] iv) throws UnInitializedException {
		if(!isInitialized()){
			throw new UnInitializedException();
		}
		byte[] generatedKey = new byte[outLen];//generated key bytes
		
		//generate the related derivation parameter for bc
		bcKdfGenerator.init(generateParameters(key.getEncoded(), iv));
		
		//generate the actual key bytes
		bcKdfGenerator.generateBytes(generatedKey, 0, outLen);
		
		//convert to key
		return new SecretKeySpec(generatedKey, "KDF");
	}


	/**
	 * @throws UnInitializedException 
	 * 
	 */
	public void generateKey(byte[] inKey, int inOff, int inLen, byte[] outKey,
			 int outOff,int outLen) throws UnInitializedException {
		if(!isInitialized()){
			throw new UnInitializedException();
		}
		//check that the offset and length are correct
		if ((inOff > inKey.length) || (inOff+inLen > inKey.length)){
			throw new ArrayIndexOutOfBoundsException("input array too short");
		}
		if ((outOff > outKey.length) || (outOff+outLen > outKey.length)){
			throw new ArrayIndexOutOfBoundsException("output array too short");
		}
		
		bcKdfGenerator.init(generateParameters(inKey,null));
		
		bcKdfGenerator.generateBytes(outKey, outOff, outLen);
		
	}
	
	/**
	 * 
	 * Generate the bc related parameters of type DerivationParameters
	 * @param shared the input key 
	 * @param iv
	 */
	private DerivationParameters generateParameters(byte[] shared, byte[] iv){
		
		if(iv==null){//iv is not provided
			
			return new ISO18033KDFParameters(shared);
		}
		else{ //iv is provided. Pass to the KDFParameters
			return new KDFParameters(shared, iv);
		}
		
	}


	
}