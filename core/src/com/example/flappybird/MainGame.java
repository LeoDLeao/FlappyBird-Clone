package com.example.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

public class MainGame extends ApplicationAdapter {

    private SpriteBatch batch;

    private Texture[] passaros;
    private Texture fundo;
    private Texture canoBaixo;
    private Texture canoTopo;
    private Texture gameOver;

    BitmapFont textPontuacao;
    BitmapFont textReiniciar;
    BitmapFont textMelhorPontuacao;

    private int pontos = 0 ;
    private int pontuacaoMaxima = 0;

    private boolean passouCano = false;

    private Circle circlePassaro;
    private Rectangle retanguloCanoCima;
    private Rectangle retanguloCanoBaixo;


    private float variacao = 0;
    private float gravidade = 0;
    private float posicaoIniciaVerticalPassaro = 0;
    private float posicaoHorizontalPassaro = 0;

    private float larguraDispositivo;
    private float alturaDispositivo;

    private Random random;

    private float posicaoCanoHorizontal;
    private float posicaoCanoVertical;
    private float espacoEntreCanos;

    Sound somVoando;
    Sound somColisao;
    Sound somPontuacao;

    Preferences preferences;

    private OrthographicCamera camera;
    private Viewport viewport;
    private final float VIRTUAL_WIDTH = 720;
    private final float VIRTUAL_HEIGHT = 1280;

    private int estadoJogo = 0;

    @Override
    public void create () {

        inicializarTextura();
        inicializarObjetos();

    }

    @Override
    public void render () {

        //limpar frames anteriores
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        desenharObjetosNaTela();
        validarPontos();
        verificarEstadoJogo();
        detectarColisoes();

    }

    private void inicializarTextura(){

        passaros = new Texture[3];
        passaros[0] = new Texture("passaro1.png");
        passaros[1] = new Texture("passaro2.png");
        passaros[2] = new Texture("passaro3.png");

        gameOver = new Texture("game_over.png");

        fundo = new Texture("fundo.png");
        canoBaixo = new Texture("cano_baixo_maior.png");
        canoTopo = new Texture("cano_topo_maior.png");
    }

    private void inicializarObjetos(){

        batch = new SpriteBatch();

        random = new Random();

        larguraDispositivo = VIRTUAL_WIDTH;
        alturaDispositivo = VIRTUAL_HEIGHT;

        posicaoIniciaVerticalPassaro = alturaDispositivo / 2;

        posicaoCanoHorizontal = larguraDispositivo;
        espacoEntreCanos = 250;

        textPontuacao = new BitmapFont();
        textPontuacao.setColor(Color.WHITE);
        textPontuacao.getData().setScale(10);

        textReiniciar = new BitmapFont();
        textReiniciar.setColor(Color.GREEN);
        textReiniciar.getData().setScale(2);

        textMelhorPontuacao = new BitmapFont();
        textMelhorPontuacao.setColor(Color.RED);
        textMelhorPontuacao.getData().setScale(2);

        circlePassaro = new Circle();
        retanguloCanoBaixo = new Rectangle();
        retanguloCanoCima = new Rectangle();

        somVoando = Gdx.audio.newSound(Gdx.files.internal("som_asa.wav"));
        somColisao = Gdx.audio.newSound(Gdx.files.internal("som_batida.wav"));
        somPontuacao = Gdx.audio.newSound(Gdx.files.internal("som_pontos.wav"));

        preferences = Gdx.app.getPreferences("flapptBird");
        pontuacaoMaxima = preferences.getInteger("pontuacaoMaxima",0);

        camera = new OrthographicCamera();
        camera.position.set(VIRTUAL_WIDTH / 2 ,VIRTUAL_HEIGHT / 2,0);
        viewport = new StretchViewport(VIRTUAL_WIDTH,VIRTUAL_HEIGHT,camera);

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width,height);
    }

    private void desenharObjetosNaTela(){

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        batch.draw(fundo, 0, 0, larguraDispositivo, alturaDispositivo);
        batch.draw(passaros[(int) variacao], 50 + posicaoHorizontalPassaro, posicaoIniciaVerticalPassaro);
        batch.draw(canoBaixo,posicaoCanoHorizontal,(alturaDispositivo / 2) - canoBaixo.getHeight() - espacoEntreCanos/2  + posicaoCanoVertical);
        batch.draw(canoTopo,posicaoCanoHorizontal, (alturaDispositivo / 2)  + espacoEntreCanos/2  + posicaoCanoVertical);

        textPontuacao.draw(batch, String.valueOf(pontos), larguraDispositivo/2, alturaDispositivo - 100);

        if(estadoJogo == 2){
            batch.draw(gameOver,(larguraDispositivo / 2) - gameOver.getWidth() / 2,alturaDispositivo / 2);
            textReiniciar.draw(batch,"Clique aqui para reiniciar! " , larguraDispositivo / 2 - 150, (alturaDispositivo / 2) - (gameOver.getHeight() / 2));
            textMelhorPontuacao.draw(batch,"Seu recorde é: " + pontuacaoMaxima + " pontos",larguraDispositivo / 2 - 150 , alturaDispositivo / 2 - gameOver.getHeight());
        }


        batch.end();
    }

    private void verificarEstadoJogo(){
        //0 inicial
        //1 jogando
        //2 colidiu

        boolean toqueTela = Gdx.input.justTouched();
        if(estadoJogo == 0){
            animarPassaro();

            if(toqueTela){
                gravidade = -15;
                estadoJogo = 1;
                somVoando.play();
            }
        }
        else if (estadoJogo == 1){
            movimentarCanos();

            if(posicaoIniciaVerticalPassaro > 0 || toqueTela) {

                posicaoIniciaVerticalPassaro = posicaoIniciaVerticalPassaro - gravidade;

                animarPassaro();

                gravidade++;
            }
            if(toqueTela){
                gravidade = -15;
                somVoando.play();
            }
        }
        else if(estadoJogo == 2){

            posicaoHorizontalPassaro -= Gdx.graphics.getDeltaTime()* 600;

            if(pontos > pontuacaoMaxima){
                pontuacaoMaxima = pontos;
                preferences.putInteger("pontucaoMaxima",pontuacaoMaxima);

                preferences.flush();
            }



            if(toqueTela){
                estadoJogo = 0;
                pontos = 0;
                gravidade = 0;

                posicaoIniciaVerticalPassaro = alturaDispositivo /2;

                posicaoCanoHorizontal = larguraDispositivo;
                posicaoHorizontalPassaro = 0;
            }
        }
    }

    private void animarPassaro(){

        variacao += Gdx.graphics.getDeltaTime() * 10;
        if(variacao > 3){
            variacao = 0;
        }
    }

    private void movimentarCanos(){

        posicaoCanoHorizontal-= Gdx.graphics.getDeltaTime() * 250;
        if(posicaoCanoHorizontal < -(canoBaixo.getWidth())){
            posicaoCanoHorizontal = larguraDispositivo;
            posicaoCanoVertical = random.nextInt(600) - 300;
            passouCano = false;
        }
    }

    private void detectarColisoes() {

        inicializarObjetosColizao();

        boolean colidiuCanoCima = Intersector.overlaps(circlePassaro,retanguloCanoCima);
        boolean colidiuCanoBaixo = Intersector.overlaps(circlePassaro,retanguloCanoBaixo);

        if ( colidiuCanoBaixo || colidiuCanoCima){
            if(estadoJogo == 1){
                somColisao.play();
                estadoJogo = 2;
                }
        }

    }

    private void inicializarObjetosColizao(){

        circlePassaro.set(50 + passaros[0].getWidth() / 2,
                posicaoIniciaVerticalPassaro + passaros[0].getHeight()/2,
                passaros[0].getWidth()/2
        );

        retanguloCanoBaixo.set(posicaoCanoHorizontal,
                alturaDispositivo/2 - canoBaixo.getHeight() - espacoEntreCanos/2 + posicaoCanoVertical,
                canoBaixo.getWidth(),
                canoBaixo.getHeight());

        retanguloCanoCima.set(posicaoCanoHorizontal,
                alturaDispositivo/2 + espacoEntreCanos/2 + posicaoCanoVertical,
                canoTopo.getWidth(),
                canoTopo.getHeight());

    }

    private void validarPontos() {

        if(posicaoCanoHorizontal < 50 - passaros[0].getWidth()){
            if(!passouCano){
                pontos++;
                passouCano = true;
                somPontuacao.play();
            }
        }

    }

    @Override
    public void dispose () {
        Gdx.app.log("dispose","Descarte de conteúdos");
    }
}
