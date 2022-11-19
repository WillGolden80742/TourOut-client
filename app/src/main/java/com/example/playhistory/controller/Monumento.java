package com.example.playhistory.controller;

public class Monumento {
    private int idMonumento;
    private String nome;
    private String descricao;
    private double latitude;
    private double longitude;
    private boolean visitado = false;

    public boolean isVisitado() {
        return visitado;
    }

    public void setVisitado(boolean visitado) {
        this.visitado = visitado;
    }

    public int getIdMonumento() {
        return idMonumento;
    }

    public void setIdMonumento(int idMonumento) {
        this.idMonumento = idMonumento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
