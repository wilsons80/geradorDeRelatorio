/**
 * Projeto   : GeradorDeRelatorio
 * Pacote    : br.jus.trt10.relatorio.util
 * Classe    : FileUtil.java
 * Autor     : wilson.souza
 * Data      : 30/09/2016
 * Descrição : 
 * 
 * Modificações 
 * 
 * Responsavel  Data        Descrição
 * ===========  ==========  =====================================================               
 *
 */

package br.com.wcs80.relatorio.util;

public class FileUtil {
	
	
	/**
	 * Retorna o nome do arquivo sem a extensão.
	 * @param nomeCompleArquivo
	 * @return String
	 */
	public static String getNameSemExtensao(String nomeCompleArquivo){
		return nomeCompleArquivo.substring(0, nomeCompleArquivo.length() -4);
	}

}


