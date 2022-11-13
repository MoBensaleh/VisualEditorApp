package com.example.assignment3.controllers;


import com.example.assignment3.models.*;
import javafx.scene.input.MouseEvent;

/**
 * The controller to handle events from the view classes.
 */
public class AppController {
    protected SMModel model;
    protected InteractionModel iModel;
    protected double prevX, prevY;
    private SMTransitionLink transitionLink;
    private SMStateNode startNode;
    private SMStateNode endNode;

    protected enum State {
        READY, PREPARE_CREATE, SELECTED, DRAGGING
    }

    protected State currentState;

    /**
     * Constructor for AppController
     */
    public AppController() {
        currentState = State.READY;
    }

    /**
     * Associate a model to the controller
     * @param newModel the drawing model information
     */
    public void setModel(SMModel newModel) {
        model = newModel;
    }

    /**
     * Associate an interaction model to the controller
     * @param newIModel interaction model
     */
    public void setInteractionModel(InteractionModel newIModel) {
        iModel = newIModel;
    }

    /**
     * Set the selected cursor
     */
    public void handleSelectedCursor(String newCursor) {
        iModel.setSelectedCursor(newCursor);
    }

    /**
     * Delete the selected item if there is one
     */
    public void handleDeleteSelected() {
        if (iModel.getSelectedItem() != null) {
            if(!iModel.getSelectedItem().isTransition()){
                SMStateNode stateNode = (SMStateNode) iModel.getSelectedItem();
                stateNode.getTransitionLinks().forEach(link -> {
                    model.deleteSelectedItem(link);
                });
            }
            model.deleteSelectedItem(iModel.getSelectedItem());
            iModel.setSelectedItem(null);
            currentState = State.READY;
        }
    }


    /**
     * Designate what the controller should do
     * based on state when a mouse is pressed
     * @param event mouse event
     */
    public void handlePressed(double normX, double normY, MouseEvent event) {
        prevX = normX;
        prevY = normY;
        switch(iModel.getSelectedCursor()){
            case "pointer" ->{
                switch (currentState){
                    case READY -> {
                        if (model.checkHit(normX, normY)) {
                            SMItem item = model.whichItem(normX, normY);
                            iModel.setSelectedItem(item);
                            currentState = State.SELECTED;
                        }
                        else {
                            iModel.setSelectedItem(null);
                            prevX = normX;
                            prevY = normY;
                            currentState = State.PREPARE_CREATE;
                        }
                    }

                    case SELECTED -> {
                        if (iModel.getSelectedItem() != null) {
                            boolean onSelectedStateNode = iModel.getSelectedItem().contains(normX, normY);
                            if (onSelectedStateNode) {
                                currentState = State.DRAGGING;
                            } else {
                                boolean onAnotherStateNode = model.checkHit(normX, normY);
                                if (onAnotherStateNode) {
                                    iModel.setSelectedItem(model.whichItem(normX, normY));
                                    model.setZOrdering(iModel.getSelectedItem());
                                }
                            }
                        } else {
                            currentState = State.READY;
                        }
                    }
                }
            }

            case "link" ->{
                switch (currentState){
                    case READY -> {
                        if (model.checkHit(normX, normY) && !model.whichItem(normX, normY).isTransition()) {
                            startNode = (SMStateNode) model.whichItem(normX, normY);
                            System.out.println(startNode);
                            currentState = State.PREPARE_CREATE;
                        }
                        else{
                            currentState = State.READY;
                        }
                    }

                    case SELECTED -> {
                        if (iModel.getSelectedItem() != null) {
                            boolean onSelectedStateNode = iModel.getSelectedItem().contains(normX, normY);
                            if (onSelectedStateNode && !model.whichItem(normX, normY).isTransition()) {
                                startNode = (SMStateNode) model.whichItem(normX, normY);
                                currentState = State.PREPARE_CREATE;
                            } else {
                                boolean onAnotherStateNode = model.checkHit(normX, normY);
                                if (onAnotherStateNode && !model.whichItem(normX, normY).isTransition()) {
                                    startNode = (SMStateNode) model.whichItem(normX, normY);
                                }
                            }
                        } else {
                            currentState = State.READY;
                        }
                    }
                }

            }
        }
    }


    /**
     * Designate what the controller should do
     * based on state when a mouse is released
     * @param event mouse event
     */


    public void handleReleased(double normX, double normY, MouseEvent event) {
        switch (currentState) {
            case PREPARE_CREATE -> {
                switch (iModel.getSelectedCursor()){
                    case "pointer" -> {
                        iModel.setSelectedItem(model.addItem(prevX, prevY, 110.0, 60.0, "Node"));
                        currentState = State.READY;
                    }
                    case "link" -> {
                        currentState = State.READY;
                    }
                }
            }
            case DRAGGING -> {
                switch(iModel.getSelectedCursor()){
                    case "pointer" -> {
                        currentState = State.SELECTED;
                    }
                    case "link" -> {
                        // check if on a state node
                        boolean hit = model.checkHit(normX, normY);
                        if (hit && !model.whichItem(normX, normY).isTransition()) {

                            model.deleteTransitionLink(transitionLink);
                            endNode = (SMStateNode) model.whichItem(normX, normY);

                            transitionLink = (SMTransitionLink) model.addItem(prevX, prevY, normX, normY, "Link");

                            //Set the start and end nodes of transition link
                            transitionLink.setEndNode(endNode);
                            transitionLink.setStartNode(startNode);

                            // Set coordinates of transition link to center
                            transitionLink.setX(transitionLink.getStartNode().getX());
                            transitionLink.setY(transitionLink.getStartNode().getY());
                            transitionLink.setX2(transitionLink.getEndNode().getX());
                            transitionLink.setY2(transitionLink.getEndNode().getY());

                            // Setting coordinates of transition node
                            transitionLink.transitionNodeX = (startNode.getX() + endNode.getX())/2;
                            transitionLink.transitionNodeY = (startNode.getY() + endNode.getY())/2;

                            transitionLink.getStartNode().addLink(transitionLink);
                            transitionLink.getEndNode().addLink(transitionLink);

                            model.setZOrdering(transitionLink.getStartNode());
                            model.setZOrdering(transitionLink.getEndNode());



                            //Made a method that notifies subscribers so the arrow is drawn on the final
                            //link
                            model.makeFinalLink(transitionLink);



                            iModel.setSelectedItem(null);
                            transitionLink = null;
                            currentState = State.READY;

                        } else {
                                model.deleteTransitionLink(transitionLink);
                                currentState = State.READY;
                        }
                    }
                }
            }

            case SELECTED -> {
                currentState = State.READY;
            }
        }
    }

    /**
     * Designate what the controller should do
     * based on state when a mouse is dragged
     * @param normX normalized x coordinate
     * @param normY normalized y coordinate
     * @param event mouse event
     */
    public void handleDragged(double normX, double normY, MouseEvent event) {
        // handle view panning
//        if (event.isSecondaryButtonDown()) {
//            double newX = event.getX()/1600;
//            double newY = event.getY()/1600;
//            model.pan(iModel.viewPort, newX, newY, noOffsetX, noOffsetY);
//        }


        switch (currentState) {
            case PREPARE_CREATE -> {
                switch (iModel.getSelectedCursor()){
                    case "pointer" ->{
                        currentState = State.READY;
                    }
                    case "link" ->{
                        // adjust the size of the transition link being drawn
                        model.deleteTransitionLink(transitionLink);
                        transitionLink = (SMTransitionLink) model.addItem(prevX, prevY, normX, normY, "Link");
                        model.notifySubscribers();
                        currentState = State.DRAGGING;
                    }
                }
            }

            case SELECTED -> {
                switch(iModel.getSelectedCursor()){
                    case "pointer" -> {
                        if (iModel.getSelectedItem() != null) {
                            boolean onStateNodeXY = iModel.getSelectedItem().contains(normX, normY);
                            if (onStateNodeXY) {
                                boolean onPrevStateNodeXY = iModel.getSelectedItem().contains(prevX, prevY);
                                if (onPrevStateNodeXY) {
                                    // get ready to move state node
                                    currentState = State.DRAGGING;
                                }
                            }
                        } else {
                            currentState = State.READY;
                        }
                    }

                    case "link" -> {
                        currentState = State.PREPARE_CREATE;
                    }
                }
            }
            case DRAGGING -> {
                // move the state node
                switch(iModel.getSelectedCursor()){
                    case "pointer" -> {
                        model.moveItem(iModel.getSelectedItem(), normX, normY);

                        model.getItems().forEach(item -> {
                            if(!item.isTransition()){
                                ((SMStateNode) item).getTransitionLinks().forEach(tLink -> {
                                    if(iModel.getSelectedItem() == tLink.getEndNode()){
                                        model.resizeTransitionLinkEnd(tLink, normX, normY);
                                    }  if (iModel.getSelectedItem() == tLink.getStartNode()) {
                                        model.resizeTransitionLinkStart(tLink, normX, normY);
                                    }
                                });
                            }
                        });
                    }
                    case "link" -> {
                        // resize the transition link
                        if(transitionLink != null){
                            model.resizeTransitionLinkEnd(transitionLink, normX, normY);
                        }
                    }
                }

            }
        }
    }
}







