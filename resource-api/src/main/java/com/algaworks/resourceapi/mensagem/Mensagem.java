package com.algaworks.resourceapi.mensagem;

import java.util.UUID;

public record Mensagem(UUID id, String texto, String autor) {
}
