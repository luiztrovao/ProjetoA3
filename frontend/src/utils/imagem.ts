// Validação das imagens no navegador, antes do envio. A API repete as mesmas regras.

export const TAMANHO_MAXIMO_IMAGEM = 1024 * 1024;

/** Devolve a mensagem de erro, ou "" se a imagem pode ser enviada (PNG ou JPG, até 1 MB). */
export const validarImagem = (arquivo: File): string => {
  if (!["image/png", "image/jpeg"].includes(arquivo.type)) return "Envie uma imagem PNG ou JPG.";
  if (arquivo.size > TAMANHO_MAXIMO_IMAGEM) return "A imagem deve ter no máximo 1 MB.";
  return "";
};
