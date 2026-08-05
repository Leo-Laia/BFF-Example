const indicadorSessao = document.querySelector("#indicador-sessao");
const textoEstado = document.querySelector("#texto-estado");
const detalheUsuario = document.querySelector("#detalhe-usuario");
const botaoEntrar = document.querySelector("#botao-entrar");
const botaoAtualizarSessao = document.querySelector("#botao-atualizar-sessao");
const botaoSair = document.querySelector("#botao-sair");
const botaoConsultarMensagens = document.querySelector("#botao-consultar-mensagens");
const formularioMensagem = document.querySelector("#formulario-mensagem");
const textoMensagem = document.querySelector("#texto-mensagem");
const botaoEnviarMensagem = document.querySelector("#botao-enviar-mensagem");
const estadoMensagens = document.querySelector("#estado-mensagens");
const listaMensagens = document.querySelector("#lista-mensagens");
const listaAtividade = document.querySelector("#lista-atividade");
const botaoLimparAtividade = document.querySelector("#botao-limpar-atividade");

let usuarioAutenticado = null;
botaoEntrar.addEventListener("click", iniciarLogin);
botaoAtualizarSessao.addEventListener("click", consultarSessao);
botaoSair.addEventListener("click", sair);
botaoConsultarMensagens.addEventListener("click", consultarMensagens);
formularioMensagem.addEventListener("submit", enviarMensagem);
botaoLimparAtividade.addEventListener("click", limparAtividade);
consultarSessao();

function iniciarLogin() {
  window.location.assign("/oauth2/authorization/keycloak");
}
async function consultarSessao() {
  definirBotoesOcupados(true);
  try {
    const resposta = await fetch("/bff/user");
    const sessaoEstaAutenticada = resposta.ok;

    if (sessaoEstaAutenticada) {
      usuarioAutenticado = await resposta.json();
      exibirSessaoAutenticada();
      adicionarAtividade("SESSÃO", "Usuário autenticado consultado no BFF.");
      await consultarMensagens();
      return;
    }

    const sessaoNaoEstaAutenticada = resposta.status === 401;
    if (sessaoNaoEstaAutenticada) {
      exibirSessaoNaoAutenticada();
      adicionarAtividade("SESSÃO", "Nenhuma sessão autenticada foi encontrada.");
      return;
    }

    throw new Error(`Falha ao consultar sessão: HTTP ${resposta.status}`);
  } catch (erro) {
    exibirErro(erro);
  } finally {
    definirBotoesOcupados(false);
  }
}
async function consultarMensagens() {
  estadoMensagens.textContent = "Consultando a Resource API...";
  estadoMensagens.hidden = false;
  listaMensagens.replaceChildren();

  try {
    const resposta = await fetch("/api/messages");
    await verificarRespostaAutenticada(resposta);
    const mensagens = await resposta.json();
    renderizarMensagens(mensagens);
    adicionarAtividade("GET", `${mensagens.length} mensagem(ns) recebida(s) pelo BFF.`);
  } catch (erro) {
    estadoMensagens.textContent = erro.message;
    adicionarAtividade("ERRO", erro.message);
  }
}
async function enviarMensagem(evento) {
  evento.preventDefault();
  const mensagem = textoMensagem.value.trim();
  const mensagemFoiInformada = mensagem.length > 0;
  if (!mensagemFoiInformada) {
    return;
  }

  botaoEnviarMensagem.disabled = true;
  try {
    const tokenCsrf = await consultarTokenCsrf();
    const resposta = await fetch("/api/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [tokenCsrf.nomeDoHeader]: tokenCsrf.token
      },
      body: JSON.stringify({ texto: mensagem })
    });
    await verificarRespostaAutenticada(resposta);
    textoMensagem.value = "";
    adicionarAtividade("POST", "Mensagem enviada com proteção CSRF.");
    await consultarMensagens();
  } catch (erro) {
    adicionarAtividade("ERRO", erro.message);
  } finally {
    atualizarEstadoDosBotoes();
  }
}
async function sair() {
  definirBotoesOcupados(true);
  try {
    const tokenCsrf = await consultarTokenCsrf();
    const resposta = await fetch("/logout", {
      method: "POST",
      headers: {
        [tokenCsrf.nomeDoHeader]: tokenCsrf.token
      }
    });
    const logoutFoiConcluido = resposta.status === 204;
    if (!logoutFoiConcluido) {
      throw new Error(`Falha ao sair: HTTP ${resposta.status}`);
    }

    usuarioAutenticado = null;
    exibirSessaoNaoAutenticada();
    listaMensagens.replaceChildren();
    estadoMensagens.textContent = "A sessão foi encerrada.";
    estadoMensagens.hidden = false;
    adicionarAtividade("LOGOUT", "Sessão invalidada pelo BFF.");
  } catch (erro) {
    exibirErro(erro);
  } finally {
    definirBotoesOcupados(false);
  }
}
async function consultarTokenCsrf() {
  const resposta = await fetch("/bff/csrf");
  await verificarRespostaAutenticada(resposta);
  return resposta.json();
}
async function verificarRespostaAutenticada(resposta) {
  const sessaoExpirou = resposta.status === 401;
  if (sessaoExpirou) {
    usuarioAutenticado = null;
    exibirSessaoNaoAutenticada();
    throw new Error("Faça login para concluir esta operação.");
  }

  if (!resposta.ok) {
    throw new Error(`A operação falhou com HTTP ${resposta.status}.`);
  }
}
function exibirSessaoAutenticada() {
  indicadorSessao.dataset.estado = "autenticado";
  textoEstado.textContent = "Autenticado";
  detalheUsuario.textContent = `${usuarioAutenticado.nome} (${usuarioAutenticado.usuario})`;
  atualizarEstadoDosBotoes();
}
function exibirSessaoNaoAutenticada() {
  indicadorSessao.dataset.estado = "nao-autenticado";
  textoEstado.textContent = "Não autenticado";
  detalheUsuario.textContent = "O browser não possui uma sessão autenticada.";
  atualizarEstadoDosBotoes();
}
function atualizarEstadoDosBotoes() {
  const existeUsuarioAutenticado = usuarioAutenticado !== null;
  botaoEntrar.disabled = existeUsuarioAutenticado;
  botaoSair.disabled = !existeUsuarioAutenticado;
  botaoEnviarMensagem.disabled = !existeUsuarioAutenticado;
}
function definirBotoesOcupados(estaoOcupados) {
  botaoAtualizarSessao.disabled = estaoOcupados;
  botaoConsultarMensagens.disabled = estaoOcupados;
  if (estaoOcupados) {
    botaoEntrar.disabled = true;
    botaoSair.disabled = true;
    botaoEnviarMensagem.disabled = true;
    return;
  }
  atualizarEstadoDosBotoes();
}
function renderizarMensagens(mensagens) {
  listaMensagens.replaceChildren();
  const nenhumaMensagemFoiEncontrada = mensagens.length === 0;
  estadoMensagens.hidden = !nenhumaMensagemFoiEncontrada;
  estadoMensagens.textContent = "Nenhuma mensagem foi encontrada.";

  mensagens.forEach((mensagem) => {
    const item = document.createElement("li");
    item.className = "mensagem";

    const texto = document.createElement("p");
    texto.textContent = mensagem.texto;

    const autor = document.createElement("span");
    autor.textContent = `Autor: ${mensagem.autor}`;

    item.append(texto, autor);
    listaMensagens.append(item);
  });
}
function adicionarAtividade(tipo, descricao) {
  const item = document.createElement("li");
  const horario = document.createElement("time");
  const identificadorTipo = document.createElement("span");
  const texto = document.createElement("span");

  horario.textContent = new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date());
  identificadorTipo.className = "tipo-atividade";
  identificadorTipo.textContent = tipo;
  texto.textContent = descricao;

  item.append(horario, identificadorTipo, texto);
  listaAtividade.prepend(item);
}
function limparAtividade() {
  listaAtividade.replaceChildren();
}
function exibirErro(erro) {
  textoEstado.textContent = "Falha de comunicação";
  detalheUsuario.textContent = erro.message;
  adicionarAtividade("ERRO", erro.message);
}
